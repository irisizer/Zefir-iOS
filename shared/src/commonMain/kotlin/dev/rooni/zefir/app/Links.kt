package dev.rooni.zefir.app

import dev.rooni.zefir.model.ProxyGroup
import dev.rooni.zefir.model.ProxyNode
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Разбор share-ссылок и подписок (порт app/ProfileImporter.kt).
 * Поддержка: vless, vmess, trojan, shadowsocks, hysteria2/hy2, hysteria, tuic, wireguard,
 * base64-список ссылок и Clash/YAML подписки (минимальный разбор proxies).
 */
object Links {

    private val schemes = listOf(
        "vless://", "vmess://", "trojan://", "ss://", "ssr://", "hysteria://",
        "hysteria2://", "hy2://", "tuic://", "wireguard://", "wg://",
    )

    fun looksLikeLink(text: String): Boolean {
        val t = text.trim()
        val lower = t.lowercase()
        return schemes.any { lower.startsWith(it) } || lower.startsWith("http://") || lower.startsWith("https://")
    }

    fun looksLikeHttp(text: String): Boolean {
        val t = text.trim().lowercase()
        return t.startsWith("http://") || t.startsWith("https://")
    }

    fun isSubscriptionUrl(text: String): Boolean {
        val t = text.trim().lowercase()
        return looksLikeHttp(t) && !t.contains("vless://") && !t.contains("trojan://")
    }

    /** Все серверы из текста подписки или из набора ссылок. */
    fun parseMany(raw: String): List<ProxyNode> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()

        // 1) похоже на Clash/YAML конфиг
        if (looksLikeYaml(text)) {
            val fromYaml = parseClashProxyNames(text)
            if (fromYaml.isNotEmpty()) return fromYaml
        }

        // 2) base64-подписка целиком
        val decoded = decodeBase64Body(text)
        val body = decoded ?: text

        val nodes = ArrayList<ProxyNode>()
        body.lineSequence().forEach { line ->
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#") || l.startsWith("//")) return@forEach
            parseLink(l)?.let { nodes.add(it) }
        }
        return nodes
    }

    /** Группы сервисов из Clash-подписки («Отдельно для сервисов»). */
    fun parseGroups(raw: String): List<ProxyGroup> {
        val text = raw.trim()
        if (!looksLikeYaml(text)) return emptyList()
        val groups = ArrayList<ProxyGroup>()
        var currentName: String? = null
        val members = ArrayList<String>()
        text.lineSequence().forEach { line ->
            val t = line.trim()
            when {
                t.startsWith("proxy-groups:") -> Unit
                t.startsWith("- name:") && line.startsWith("  - name:") -> {
                    currentName?.let { groups.add(ProxyGroup(it, "select", "", members.toList())) }
                    members.clear()
                    currentName = t.removePrefix("- name:").trim().trim('"', '\'')
                }
            }
        }
        currentName?.let { groups.add(ProxyGroup(it, "select", "", members.toList())) }
        return groups.filter { it.name.isNotBlank() }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64Body(text: String): String? {
        val compact = text.filterNot { it == '\n' || it == '\r' || it == ' ' }
        if (compact.length < 16) return null
        if (compact.contains("://")) return null
        return runCatching {
            val normalized = compact.replace('-', '+').replace('_', '/')
            val padded = when (normalized.length % 4) {
                2 -> "$normalized=="
                3 -> "$normalized="
                0 -> normalized
                else -> return null
            }
            val decoded = Base64.decode(padded)
            val s = decoded.decodeToString()
            if (s.contains("://")) s else null
        }.getOrNull()
    }

    private fun looksLikeYaml(text: String): Boolean =
        text.contains("proxies:") && !text.contains("://", ignoreCase = false) ||
            text.contains("proxies:")

    private fun parseClashProxyNames(text: String): List<ProxyNode> {
        val nodes = ArrayList<ProxyNode>()
        var inProxies = false
        var name: String? = null
        var type = ""
        var server = ""
        var port = 0
        fun flush() {
            val n = name ?: return
            if (n.isBlank()) return
            nodes.add(
                ProxyNode(
                    name = n,
                    type = type.ifBlank { "clash" },
                    server = server,
                    port = port,
                    flag = flagOf(n),
                )
            )
            name = null; type = ""; server = ""; port = 0
        }
        text.lineSequence().forEach { line ->
            val t = line.trim()
            when {
                t == "proxies:" -> inProxies = true
                inProxies && (t.startsWith("proxy-groups:") || t.startsWith("rules:")) -> {
                    flush(); inProxies = false
                }
                inProxies && t.startsWith("- name:") -> {
                    flush()
                    name = t.removePrefix("- name:").trim().trim('"', '\'')
                }
                inProxies && t.startsWith("type:") -> type = t.removePrefix("type:").trim()
                inProxies && t.startsWith("server:") -> server = t.removePrefix("server:").trim()
                inProxies && t.startsWith("port:") -> port = t.removePrefix("port:").trim().toIntOrNull() ?: 0
            }
        }
        flush()
        return nodes
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun parseLink(link: String): ProxyNode? {
        val l = link.trim()
        return when {
            l.startsWith("vless://", true) -> parseUrlLike(l, "vless")
            l.startsWith("trojan://", true) -> parseUrlLike(l, "trojan")
            l.startsWith("hysteria2://", true) || l.startsWith("hy2://", true) -> parseUrlLike(l, "hysteria2")
            l.startsWith("hysteria://", true) -> parseUrlLike(l, "hysteria")
            l.startsWith("tuic://", true) -> parseUrlLike(l, "tuic")
            l.startsWith("ss://", true) -> parseSs(l)
            l.startsWith("vmess://", true) -> parseVmess(l)
            l.startsWith("wireguard://", true) || l.startsWith("wg://", true) -> parseUrlLike(l, "wireguard")
            else -> null
        }
    }

    private fun parseUrlLike(link: String, type: String): ProxyNode? {
        val rest = link.substringAfter("://")
        val hashIndex = rest.indexOf('#')
        val name = if (hashIndex >= 0) decode(rest.substring(hashIndex + 1)) else ""
        val withoutName = if (hashIndex >= 0) rest.substring(0, hashIndex) else rest
        val authority = withoutName.substringBefore('?').substringAfter('@')
        if (authority.isBlank()) return null
        val host = authority.substringBefore(':')
        val port = authority.substringAfter(':', "").toIntOrNull() ?: 443
        val display = name.ifBlank { "$host:$port" }
        return ProxyNode(
            name = display,
            type = type,
            server = host.removePrefix("[").removeSuffix("]"),
            port = port,
            link = link,
            flag = flagOf(display),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseSs(link: String): ProxyNode? {
        val rest = link.removePrefix("ss://")
        val name = if (rest.contains('#')) decode(rest.substringAfter('#')) else ""
        val body = rest.substringBefore('#').substringBefore('?')
        val decoded = runCatching {
            val padded = body.replace('-', '+').replace('_', '/').let {
                it + "=".repeat((4 - it.length % 4) % 4)
            }
            Base64.decode(padded).decodeToString()
        }.getOrNull()
        val authority = decoded?.substringAfter('@') ?: body.substringAfter('@', "")
        if (authority.isBlank()) return null
        val host = authority.substringBefore(':')
        val port = authority.substringAfter(':', "8388").toIntOrNull() ?: 8388
        val display = name.ifBlank { "$host:$port" }
        return ProxyNode(
            name = display,
            type = "ss",
            server = host,
            port = port,
            link = link,
            flag = flagOf(display),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseVmess(link: String): ProxyNode? {
        val body = link.removePrefix("vmess://").substringBefore('#')
        val json = runCatching {
            val padded = body.replace('-', '+').replace('_', '/').let {
                it + "=".repeat((4 - it.length % 4) % 4)
            }
            Base64.decode(padded).decodeToString()
        }.getOrNull() ?: return null
        val name = extractJsonString(json, "ps").ifBlank { extractJsonString(json, "add") }
        val host = extractJsonString(json, "add")
        val port = extractJsonNumber(json, "port")
        return ProxyNode(
            name = name.ifBlank { host },
            type = "vmess",
            server = host,
            port = port,
            link = link,
            flag = flagOf(name),
        )
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1).orEmpty()
    }

    private fun extractJsonNumber(json: String, key: String): Int {
        val regex = Regex("\"$key\"\\s*:\\s*\"?([0-9]+)\"?")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 443
    }

    private fun decode(value: String): String = runCatching { percentDecode(value) }.getOrDefault(value)

    private fun percentDecode(value: String): String {
        val bytes = ArrayList<Byte>()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 < value.length) {
                val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) {
                    bytes.add(hex.toByte()); i += 3; continue
                }
            }
            if (c == '+') bytes.add(' '.code.toByte()) else bytes.add(c.toString().encodeToByteArray()[0])
            i++
        }
        return bytes.toByteArray().decodeToString()
    }

    /** Флаг из имени сервера: региональные индикаторы в начале строки. */
    fun flagOf(name: String): String? {
        val indicators = name.takeWhile { it.code in 0x1F1E6..0x1F1FF }
        return indicators.ifBlank { null }
    }

    /** Имя без флага — для списка серверов. */
    fun withoutFlag(name: String): String {
        val flag = flagOf(name) ?: return name.trim()
        return name.removePrefix(flag).trim().trim('-', '·', '|', ' ').trim()
    }
}
