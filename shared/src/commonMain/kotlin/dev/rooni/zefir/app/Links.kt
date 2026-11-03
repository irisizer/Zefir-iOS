package dev.rooni.zefir.app

import dev.rooni.zefir.model.ProxyGroup
import dev.rooni.zefir.model.ProxyNode
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Разбор share-ссылок и подписок (порт app/ProfileImporter.kt).
 * Поддержка: vless (tls/reality/ws/grpc), vmess, trojan, shadowsocks,
 * hysteria2/hy2, hysteria, tuic, wireguard, base64-подписка и Clash/YAML.
 *
 * Кроме имени/адреса вынимаем все параметры ссылки в [ProxyNode.params] и
 * логин в [ProxyNode.user] — из этого собирается конфиг sing-box.
 */
object Links {

    private val schemes = listOf(
        "vless://", "vmess://", "trojan://", "ss://", "ssr://", "hysteria://",
        "hysteria2://", "hy2://", "tuic://", "wireguard://", "wg://",
    )

    fun looksLikeLink(text: String): Boolean {
        val lower = text.trim().lowercase()
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

        if (looksLikeYaml(text)) {
            val fromYaml = parseClashProxyNames(text)
            if (fromYaml.isNotEmpty()) return fromYaml
        }

        val body = decodeBase64Body(text) ?: text
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
            b64decode(compact).let { if (it.contains("://")) it else null }
        }.getOrNull()
    }

    private fun looksLikeYaml(text: String): Boolean = text.contains("proxies:")

    private fun parseClashProxyNames(text: String): List<ProxyNode> {
        val nodes = ArrayList<ProxyNode>()
        var inProxies = false
        var name: String? = null
        var type = ""
        var server = ""
        var port = 0
        var uuid = ""
        var password = ""
        var extra = mutableMapOf<String, String>()
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
                    user = uuid.ifBlank { password },
                    params = extra.toMap(),
                )
            )
            name = null; type = ""; server = ""; port = 0; uuid = ""; password = ""; extra = mutableMapOf()
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
                inProxies && t.startsWith("uuid:") -> uuid = t.removePrefix("uuid:").trim().trim('"')
                inProxies && t.startsWith("password:") -> password = t.removePrefix("password:").trim().trim('"')
                inProxies && t.startsWith("sni:") -> extra["sni"] = t.removePrefix("sni:").trim().trim('"')
                inProxies && t.startsWith("servername:") -> extra["sni"] = t.removePrefix("servername:").trim().trim('"')
                inProxies && t.startsWith("network:") -> extra["type"] = t.removePrefix("network:").trim()
            }
        }
        flush()
        return nodes
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun parseLink(link: String): ProxyNode? {
        val l = link.trim()
        return when {
            l.startsWith("vless://", true) -> parseVlessLike(l, "vless")
            l.startsWith("trojan://", true) -> parseVlessLike(l, "trojan")
            l.startsWith("hysteria2://", true) || l.startsWith("hy2://", true) -> parseVlessLike(l, "hysteria2")
            l.startsWith("hysteria://", true) -> parseVlessLike(l, "hysteria")
            l.startsWith("tuic://", true) -> parseTuic(l)
            l.startsWith("ss://", true) -> parseSs(l)
            l.startsWith("vmess://", true) -> parseVmess(l)
            l.startsWith("wireguard://", true) || l.startsWith("wg://", true) -> parseVlessLike(l, "wireguard")
            else -> null
        }
    }

    /** Единый парсер scheme://user@host:port?params#name. */
    private fun parseVlessLike(link: String, type: String): ProxyNode? {
        val rest = link.substringAfter("://")
        val hashIndex = rest.indexOf('#')
        val name = if (hashIndex >= 0) decodeUrl(rest.substring(hashIndex + 1)) else ""
        val withoutName = if (hashIndex >= 0) rest.substring(0, hashIndex) else rest

        val queryPart = withoutName.substringAfter('?', "")
        val beforeQuery = withoutName.substringBefore('?')

        val atSign = beforeQuery.lastIndexOf('@')
        if (atSign < 0) return null
        val userInfo = beforeQuery.substring(0, atSign)
        val authority = beforeQuery.substring(atSign + 1)
        val hostPart = authority.substringBefore(':')
        val portPart = authority.substringAfter(':', "")
        if (hostPart.isBlank()) return null
        val port = portPart.toIntOrNull() ?: defaultPort(type)

        val params = parseQuery(queryPart)
        val display = name.ifBlank { "$hostPart:$port" }
        return ProxyNode(
            name = display,
            type = type,
            server = hostPart.removePrefix("[").removeSuffix("]"),
            port = port,
            link = link,
            flag = flagOf(display),
            user = decodeUrl(userInfo.substringBefore(':')),
            params = params,
        )
    }

    private fun parseTuic(link: String): ProxyNode? {
        val node = parseVlessLike(link, "tuic") ?: return null
        val rest = link.substringAfter("://").substringBefore('#').substringBefore('?')
        val atSign = rest.lastIndexOf('@')
        val userInfo = rest.substring(0, atSign)
        val uuid = decodeUrl(userInfo.substringBefore(':'))
        val password = decodeUrl(userInfo.substringAfter(':', ""))
        return node.copy(user = uuid, params = node.params + ("password" to password))
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseSs(link: String): ProxyNode? {
        val rest = link.removePrefix("ss://")
        val name = if (rest.contains('#')) decodeUrl(rest.substringAfter('#')) else ""
        val body = rest.substringBefore('#').substringBefore('?')

        // вариант 1: ss://base64(method:pass@host:port)
        val decodedWhole = runCatching { b64decode(body) }.getOrNull()
        if (decodedWhole != null && decodedWhole.contains('@')) {
            val methodPass = decodedWhole.substringBefore('@')
            val authority = decodedWhole.substringAfter('@')
            val host = authority.substringBefore(':')
            val port = authority.substringAfter(':', "8388").toIntOrNull() ?: 8388
            val method = decodeUrl(methodPass.substringBefore(':'))
            val password = decodeUrl(methodPass.substringAfter(':', ""))
            val display = name.ifBlank { "$host:$port" }
            return ProxyNode(
                name = display, type = "ss", server = host, port = port, link = link,
                flag = flagOf(display), user = password, params = mapOf("method" to method),
            )
        }

        // вариант 2: ss://base64(method:pass)@host:port
        val base64Part = body.substringBefore('@')
        val authority = body.substringAfter('@', "")
        if (authority.isBlank()) return null
        val host = authority.substringBefore(':')
        val port = authority.substringAfter(':', "8388").toIntOrNull() ?: 8388
        val decoded = runCatching { b64decode(base64Part) }.getOrNull() ?: return null
        val method = decodeUrl(decoded.substringBefore(':'))
        val password = decodeUrl(decoded.substringAfter(':', ""))
        val display = name.ifBlank { "$host:$port" }
        return ProxyNode(
            name = display, type = "ss", server = host, port = port, link = link,
            flag = flagOf(display), user = password, params = mapOf("method" to method),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun parseVmess(link: String): ProxyNode? {
        val body = link.removePrefix("vmess://").substringBefore('#')
        val json = runCatching { b64decode(body) }.getOrNull() ?: return null
        val name = extractJsonString(json, "ps")
        val host = extractJsonString(json, "add")
        val port = extractJsonNumber(json, "port")
        val uuid = extractJsonString(json, "id")
        val aid = extractJsonString(json, "aid").ifBlank { "0" }
        val params = buildMap {
            put("type", extractJsonString(json, "net").ifBlank { "tcp" })
            put("path", extractJsonString(json, "path"))
            put("host", extractJsonString(json, "host"))
            put("sni", extractJsonString(json, "sni"))
            put("tls", extractJsonString(json, "tls"))
            put("fp", extractJsonString(json, "fp"))
            put("scy", extractJsonString(json, "scy"))
            put("aid", aid)
        }
        val display = name.ifBlank { host }
        return ProxyNode(
            name = display, type = "vmess", server = host, port = port, link = link,
            flag = flagOf(display), user = uuid, params = params,
        )
    }

    private fun defaultPort(type: String): Int = when (type) {
        "hysteria", "hysteria2" -> 443
        "trojan" -> 443
        "wireguard" -> 51820
        else -> 443
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        query.split('&').forEach { pair ->
            if (pair.isBlank()) return@forEach
            val key = decodeUrl(pair.substringBefore('='))
            val value = decodeUrl(pair.substringAfter('=', ""))
            if (key.isNotBlank()) map[key] = value
        }
        return map
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun b64decode(value: String): String {
        val normalized = value.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.decode(padded).decodeToString()
    }

    private fun extractJsonString(json: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1).orEmpty()
    }

    private fun extractJsonNumber(json: String, key: String): Int =
        extractJsonString(json, key).toIntOrNull() ?: 443

    fun decodeUrl(value: String): String {
        if (!value.contains('%') && !value.contains('+')) return value
        return runCatching { percentDecode(value) }.getOrDefault(value)
    }

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
