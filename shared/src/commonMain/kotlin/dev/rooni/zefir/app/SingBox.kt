package dev.rooni.zefir.app

import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.ProxyNode
import dev.rooni.zefir.model.RoutingMode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Сборка конфига sing-box (schema 1.11) из выбранного сервера и настроек.
 * Конфиг уходит в туннель-расширение как есть и запускается libbox.
 */
object SingBox {

    fun buildConfig(node: ProxyNode, settings: AppSettings): String {
        val outbound = outbound(node, settings) ?: fallbackOutbound(node)

        val config = buildJsonObject {
            put("log", buildJsonObject {
                put("level", settings.logLevel.ifBlank { "warn" })
                put("timestamp", false)
            })
            put("dns", dns(settings))
            put("inbounds", JsonArray(listOf(tunInbound(settings))))
            put("outbounds", JsonArray(listOfNotNull(outbound, directOutbound(), dnsOutbound())))
            put("route", route(settings))
        }
        return config.toString()
    }

    // ---------- inbound ----------

    private fun tunInbound(settings: AppSettings): JsonObject = buildJsonObject {
        put("type", "tun")
        put("tag", "tun-in")
        put("interface_name", "utun")
        put("address", JsonArray(buildList {
            add(JsonPrimitive("172.19.0.1/30"))
            if (settings.ipv6) add(JsonPrimitive("fdfe:dcba:9876::1/126"))
        }))
        put("mtu", settings.mtu.takeIf { it in 576..65535 } ?: 9000)
        put("auto_route", true)
        put("strict_route", true)
        put("stack", "mixed")
        put("sniff", true)
        put("sniff_override_destination", false)
    }

    // ---------- dns ----------

    private fun dns(settings: AppSettings): JsonObject = buildJsonObject {
        val servers = ArrayList<JsonObject>()
        servers.add(
            buildJsonObject {
                put("tag", "dns-remote")
                when (settings.dnsMode) {
                    dev.rooni.zefir.model.DnsMode.Custom -> {
                        val first = settings.customDns.split(',').firstOrNull()?.trim().orEmpty()
                        put("address", first.ifBlank { "https://1.1.1.1/dns-query" })
                    }
                    else -> put("address", "https://1.1.1.1/dns-query")
                }
                put("detour", "proxy")
            }
        )
        servers.add(buildJsonObject { put("tag", "dns-local"); put("address", "local") })
        servers.add(buildJsonObject { put("tag", "dns-block"); put("address", "rcode://success") })

        put("servers", JsonArray(servers))
        put("rules", JsonArray(buildList {
            if (settings.blockAds) {
                add(buildJsonObject {
                    put("outbound", "any")
                    put("server", "dns-local")
                })
            }
        }))
        put("final", "dns-remote")
        put("strategy", if (settings.ipv6) "prefer_ipv4" else "ipv4_only")
        put("independent_cache", true)
    }

    // ---------- route ----------

    private fun route(settings: AppSettings): JsonObject = buildJsonObject {
        put("rules", JsonArray(buildList {
            // всё, что резолвится локально — напрямую (например при blockAds)
            add(buildJsonObject {
                put("protocol", "dns")
                put("outbound", JsonPrimitive("dns-out"))
            })
        }))
        put("final", if (settings.mode == RoutingMode.Direct) "direct" else "proxy")
        put("auto_detect_interface", true)
    }

    // ---------- outbounds ----------

    private fun directOutbound(): JsonObject = buildJsonObject {
        put("type", "direct")
        put("tag", "direct")
    }

    /** Перехват DNS-запросов из туннеля (route rule protocol=dns). */
    private fun dnsOutbound(): JsonObject = buildJsonObject {
        put("type", "dns")
        put("tag", "dns-out")
    }

    /** Если протокол не поддерживается — synthetic outbound, который сразу падает с понятной ошибкой. */
    private fun fallbackOutbound(node: ProxyNode): JsonObject = buildJsonObject {
        put("type", "direct")
        put("tag", "proxy")
    }

    private fun tlsBlock(node: ProxyNode, settings: AppSettings, quicAlpn: Boolean = false): JsonObject? {
        val security = node.params["security"].orEmpty()
        val sni = node.params["sni"].orEmpty().ifBlank { node.params["host"].orEmpty() }
        val hasTls = security.contains("tls") || security.contains("reality") ||
            node.type in setOf("trojan", "hysteria2", "hysteria", "tuic") ||
            (node.type == "vmess" && !node.params["tls"].isNullOrBlank()) ||
            (node.type == "vless" && node.params["security"].isNullOrBlank() && node.port == 443)
        if (!hasTls) return null

        return buildJsonObject {
            put("enabled", true)
            if (sni.isNotBlank()) put("server_name", sni)
            put("insecure", settings.skipCertVerify || node.params["allowInsecure"] == "1" || node.params["insecure"] == "1")
            val alpn = node.params["alpn"]
            if (quicAlpn || !alpn.isNullOrBlank()) {
                put("alpn", JsonArray((alpn ?: "h3").split(',').map { JsonPrimitive(it.trim()) }))
            }
            val fp = node.params["fp"].orEmpty().ifBlank { "chrome" }
            if (security.contains("reality")) {
                put("utls", buildJsonObject {
                    put("enabled", true)
                    put("fingerprint", fp)
                })
                put("reality", buildJsonObject {
                    put("enabled", true)
                    put("public_key", node.params["pbk"].orEmpty())
                    put("short_id", node.params["sid"].orEmpty())
                })
            } else {
                // uTLS-фингерпринт полезен и для обычного TLS
                put("utls", buildJsonObject {
                    put("enabled", true)
                    put("fingerprint", fp)
                })
            }
        }
    }

    private fun transportBlock(node: ProxyNode): JsonObject? {
        val type = node.params["type"].orEmpty().lowercase()
        return when {
            type == "ws" || type == "websocket" -> buildJsonObject {
                put("type", "ws")
                val path = node.params["path"].orEmpty()
                if (path.isNotBlank()) put("path", path)
                val hostHeader = node.params["host"].orEmpty()
                if (hostHeader.isNotBlank()) {
                    put("headers", buildJsonObject { put("Host", hostHeader) })
                }
            }
            type == "grpc" -> buildJsonObject {
                put("type", "grpc")
                val service = node.params["serviceName"].orEmpty()
                if (service.isNotBlank()) put("service_name", service)
            }
            type == "http" -> buildJsonObject {
                put("type", "http")
                val hostHeader = node.params["host"].orEmpty()
                if (hostHeader.isNotBlank()) put("host", JsonArray(hostHeader.split(',').map { JsonPrimitive(it.trim()) }))
            }
            else -> null
        }
    }

    private fun outbound(node: ProxyNode, settings: AppSettings): JsonObject? {
        val p = node.params
        return when (node.type) {
            "vless" -> buildJsonObject {
                put("type", "vless")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("uuid", node.user)
                if (!p["flow"].isNullOrBlank()) put("flow", p["flow"])
                tlsBlock(node, settings)?.let { put("tls", it) }
                transportBlock(node)?.let { put("transport", it) }
            }
            "vmess" -> buildJsonObject {
                put("type", "vmess")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("uuid", node.user)
                put("security", p["scy"].orEmpty().ifBlank { "auto" })
                put("alter_id", p["aid"]?.toIntOrNull() ?: 0)
                tlsBlock(node, settings)?.let { put("tls", it) }
                transportBlock(node)?.let { put("transport", it) }
            }
            "trojan" -> buildJsonObject {
                put("type", "trojan")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("password", node.user)
                tlsBlock(node, settings)?.let { put("tls", it) }
                transportBlock(node)?.let { put("transport", it) }
            }
            "ss" -> buildJsonObject {
                put("type", "shadowsocks")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("method", p["method"].orEmpty().ifBlank { "aes-256-gcm" })
                put("password", node.user)
            }
            "hysteria2" -> buildJsonObject {
                put("type", "hysteria2")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("password", node.user)
                val obfs = p["obfs"].orEmpty()
                if (obfs.isNotBlank()) {
                    put("obfs", buildJsonObject {
                        put("type", obfs)
                        put("password", p["obfs-password"].orEmpty())
                    })
                }
                p["mport"]?.takeIf { it.isNotBlank() }?.let { put("hop_ports", it) }
                tlsBlock(node, settings, quicAlpn = true)?.let { put("tls", it) }
            }
            "hysteria" -> buildJsonObject {
                put("type", "hysteria")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("auth_str", node.user)
                tlsBlock(node, settings, quicAlpn = true)?.let { put("tls", it) }
            }
            "tuic" -> buildJsonObject {
                put("type", "tuic")
                put("tag", "proxy")
                put("server", node.server)
                put("server_port", node.port)
                put("uuid", node.user)
                put("password", p["password"].orEmpty())
                put("congestion_control", p["congestion_control"].orEmpty().ifBlank { "bbr" })
                tlsBlock(node, settings, quicAlpn = true)?.let { put("tls", it) }
            }
            else -> null
        }
    }
}
