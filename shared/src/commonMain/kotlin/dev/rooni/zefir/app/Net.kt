package dev.rooni.zefir.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/** Загрузка подписки (порт app/SubscriptionClient.kt). */
object SubscriptionClient {

    suspend fun fetch(url: String, client: String?, customClient: String?): String {
        val http: HttpClient = httpClient()
        val ua = when {
            customClient.isNullOrBlank() && client.isNullOrBlank() -> DEFAULT_UA
            client == "custom" && !customClient.isNullOrBlank() -> customClient
            client == "off" -> DEFAULT_UA
            !client.isNullOrBlank() -> client
            else -> DEFAULT_UA
        }
        return try {
            http.get(url) {
                header("User-Agent", ua)
                header("Accept", "*/*")
            }.bodyAsText()
        } finally {
            http.close()
        }
    }

    const val DEFAULT_UA = "Zefir/0.1.4"
}

expect fun httpClient(): HttpClient

expect fun nowMillis(): Long

/** TCP-пинг сервера (как TcpPinger в оригинале). */
expect suspend fun tcpPing(host: String, port: Int): Int?

object TcpPing {
    suspend fun ping(host: String, port: Int): Int? = tcpPing(host, port)
}