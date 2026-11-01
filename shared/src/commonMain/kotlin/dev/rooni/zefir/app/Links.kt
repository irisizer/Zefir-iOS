package dev.rooni.zefir.app

import dev.rooni.zefir.model.ProxyNode
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Разбор share-ссылок и подписок (порт app/ProfileImporter.kt). */
object Links {

    private val schemes = listOf("vless://", "vmess://", "ss://", "ssr://", "trojan://", "hysteria://", "hysteria2://", "hy2://", "tuic://")

    fun looksLikeLink(text: String): Boolean {
        val t = text.trim()
        return schemes.any { t.startsWith(it, ignoreCase = true) } || t.startsWith("http://") || t.startsWith("https://")
    }

    fun looksLikeHttp(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("http://") || t.startsWith("https://")
    }

    /**
     * Разбирает содержимое подписки: base64-блок ссылок, plain-список ссылок,
     * либо Clash/WireGuard конфиг (в этом случае серверы достаём по `- name:`).
     */
    fun parseMany(body: String): List<ProxyNode> {
        val text = body.trim()
        if (text.isEmpty()) return emptyList()
        if (text.startsWith("proxies:") || text.contains("\nproxies:")) return parseClash(text)
        if (text.startsWith("[Interface]")) return parseWireGuard(text)

        val decoded = decodeBase64OrNull(text)
        val source = decoded ?: text
        val lines = source.split('\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }
        val out = ArrayList<ProxyNode>()
        for (line in lines) {
            val node = parseLink(line)
            if (node != null) out.add(node)
        }
        if (out.isEmpty()) {
            // возможно это одиночная ссылка без переносов
            parseLink(text)?.let { out.add(it) }
        }
        return out
    }

    fun parseLink(raw: String): ProxyNode? {
        val link = raw.trim()
        if (link.isEmpty()) return null
        return when {
            link.startsWith("vmess://", true) -> parseVmess(link)
            link.startsWith("vless://", true) -> parseVless(link)
            link.startsWith("trojan://", true) -> parseTrojan(link)
            link.startsWith("ss://", true) -> parseShadowsocks(link)
            link.startsWith("hysteria://", true) || link.startsWith("hysteria2://", true) || link.startsWith("hy2://", true) ->
                parseHysteria(link)
            link.startsWith("tuic://", true) -> parseGeneric("tuic", link)
            else -> null
        }
    }

    private data class Parsed(val userInfo: String, val host: String, val port: Int, val name: String?)

    private fun splitUrl(link: String): Parsed? {
        val afterScheme = link.substringAfter("://")
        val hashIdx = afterScheme.indexOf('#')
        val name = if (hashIdx >= 0) decode(afterScheme.substring(hashIdx + 1)) else null
        val noFragment = if (hashIdx >= 0) afterScheme.substring(0, hashIdx) else afterScheme
        val queryIdx = noFragment.indexOf('?')
        val authority = if (queryIdx >= 0) noFragment.substring(0, queryIdx) else noFragment
        val at = authority.lastIndexOf('@')
        if (at < 0) return null
        val userInfo = authority.substring(0, at)
        val hostPort = authority.substring(at + 1)
        val colon = hostPort.lastIndexOf(':')
        if (colon < 0) return null
        val host = hostPort.substring(0, colon).removeSurrounding("[", "]")
        val port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
        return Parsed(userInfo, host, port, name)
    }

    private fun parseVless(link: String): ProxyNode? {
        val p = splitUrl(link) ?: return null
        return node(link, p, "vless")
    }

    private fun parseTrojan(link: String): ProxyNode? {
        val p = splitUrl(link) ?: return null
        return node(link, p, "trojan")
    }

    private fun parseHysteria(link: String): ProxyNode? {
        val p = splitUrl(link) ?: return null
        return node(link, p, "hysteria2")
    }

    private fun parseGeneric(type: String, link: String): ProxyNode? {
        val p = splitUrl(link) ?: return null
        return node(link, p, type)
    }

    private fun node(link: String, p: Parsed, type: String): ProxyNode {
        val label = p.name?.trim().orEmpty().ifEmpty { "${p.host}:${p.port}" }
        val flag = flagOf(label)
        return ProxyNode(
            name = label,
            type = type,
            host = p.host,
            port = p.port,
            country = countryOf(flag),
            flag = flag,
            link = link,
        )
    }

    private fun parseShadowsocks(link: String): ProxyNode? {
        val afterScheme = link.substringAfter("://")
        val hashIdx = afterScheme.indexOf('#')
        val name = if (hashIdx >= 0) decode(afterScheme.substring(hashIdx + 1)) else null
        var body = if (hashIdx >= 0) afterScheme.substring(0, hashIdx) else afterScheme
        body = body.substringBefore('?')
        val at = body.lastIndexOf('@')
        if (at >= 0) {
            val hostPort = body.substring(at + 1)
            val colon = hostPort.lastIndexOf(':')
            if (colon < 0) return null
            val host = hostPort.substring(0, colon)
            val port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
            val label = name?.trim().orEmpty().ifEmpty { "$host:$port" }
            val flag = flagOf(label)
            return ProxyNode(label, "ss", host, port, countryOf(flag), flag, link)
        }
        val decoded = decodeBase64OrNull(body) ?: return null
        val at2 = decoded.lastIndexOf('@')
        if (at2 < 0) return null
        val hostPort = decoded.substring(at2 + 1)
        val colon = hostPort.lastIndexOf(':')
        if (colon < 0) return null
        val host = hostPort.substring(0, colon)
        val port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
        val label = name?.trim().orEmpty().ifEmpty { "$host:$port" }
        val flag = flagOf(label)
        return ProxyNode(label, "ss", host, port, countryOf(flag), flag, link)
    }

    private fun parseVmess(link: String): ProxyNode? {
        val body = link.substringAfter("://")
        val json = decodeBase64OrNull(body) ?: return null
        val ps = jsonField(json, "ps") ?: "vmess"
        val add = jsonField(json, "add") ?: return null
        val port = jsonField(json, "port")?.toIntOrNull() ?: 443
        val flag = flagOf(ps)
        return ProxyNode(ps, "vmess", add, port, countryOf(flag), flag, link)
    }

    private fun parseClash(text: String): List<ProxyNode> {
        val out = ArrayList<ProxyNode>()
        var name: String? = null
        var type = "clash"
        var server: String? = null
        var port = 0
        fun flush() {
            val n = name
            val s = server
            if (n != null && s != null) {
                val flag = flagOf(n)
                out.add(ProxyNode(n, type, s, port, countryOf(flag), flag, "clash://$n"))
            }
            name = null; type = "clash"; server = null; port = 0
        }
        for (lineRaw in text.split('\n')) {
            val line = lineRaw.trim()
            when {
                line.startsWith("- name:") || line.startsWith("- {name:") -> {
                    flush()
                    name = line.substringAfter("name:").trim().trim('"', '\'')
                }
                line.startsWith("name:") -> name = line.substringAfter("name:").trim().trim('"', '\'')
                line.startsWith("type:") -> type = line.substringAfter("type:").trim().trim('"', '\'')
                line.startsWith("server:") -> server = line.substringAfter("server:").trim().trim('"', '\'')
                line.startsWith("port:") -> port = line.substringAfter("port:").trim().toIntOrNull() ?: 0
            }
        }
        flush()
        return out
    }

    private fun parseWireGuard(text: String): List<ProxyNode> {
        // WireGuard-конфиг = один сервер
        val endpoint = Regex("Endpoint\\s*=\\s*([^:\\s]+):(\\d+)").find(text)
        if (endpoint == null) return emptyList()
        val host = endpoint.groupValues[1]
        val port = endpoint.groupValues[2].toIntOrNull() ?: 51820
        val label = "WireGuard $host"
        return listOf(ProxyNode(label, "wireguard", host, port, null, null, text))
    }

    // ---- утилиты ----

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64OrNull(text: String): String? {
        val cleaned = text.filterNot { it.isWhitespace() }
        if (cleaned.isEmpty()) return null
        val padded = when (cleaned.length % 4) {
            2 -> "$cleaned=="
            3 -> "$cleaned="
            0 -> cleaned
            else -> return null
        }
        return runCatching {
            Base64.decode(padded).decodeToString()
        }.getOrNull()?.takeIf { it.contains("://") || it.contains("proxies:") }
    }

    private fun decode(s: String): String {
        val withPlus = s.replace("+", " ")
        return runCatching { percentDecode(withPlus) }.getOrDefault(withPlus)
    }

    private fun percentDecode(s: String): String {
        val bytes = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) {
                    bytes.add(hex.toByte()); i += 3; continue
                }
            }
            bytes.add(c.code.toByte()); i++
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun jsonField(json: String, field: String): String? {
        val m = Regex("\"$field\"\\s*:\\s*\"([^\"]*)\"").find(json) ?: return null
        return m.groupValues[1]
    }

    /** Флаг из эмодзи-индикаторов в начале названия сервера. */
    fun flagOf(label: String): String? {
        val cps = label.zefirCodePoints()
        if (cps.size < 2) return null
        val a = cps[0]
        val b = cps[1]
        if (a in 0x1F1E6..0x1F1FF && b in 0x1F1E6..0x1F1FF) {
            return codePointToString(a) + codePointToString(b)
        }
        return null
    }

    private fun countryOf(flag: String?): String? {
        if (flag == null) return null
        return CountryNames.nameOf(flag)
    }
}

internal fun codePointToString(cp: Int): String {
    if (cp <= 0xFFFF) return cp.toChar().toString()
    val v = cp - 0x10000
    val hi = 0xD800 + (v shr 10)
    val lo = 0xDC00 + (v and 0x3FF)
    return hi.toChar().toString() + lo.toChar().toString()
}

internal fun String.zefirCodePoints(): IntArray {
    val out = ArrayList<Int>(length)
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            val cp = 0x10000 + ((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00)
            out.add(cp)
            i += 2
        } else {
            out.add(c.code)
            i++
        }
    }
    return out.toIntArray()
}