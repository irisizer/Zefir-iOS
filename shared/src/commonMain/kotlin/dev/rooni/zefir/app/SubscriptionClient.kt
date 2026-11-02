package dev.rooni.zefir.app

import dev.rooni.zefir.model.SubscriptionInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class SubscriptionFetch(
    val body: String,
    val info: SubscriptionInfo,
    val client: String,
)

/** Скачивание подписки + метаданные из заголовков (app/SubscriptionClient.kt). */
object SubscriptionClient {

    private val client: HttpClient by lazy { createHttpClient() }

    private val userAgents = listOf(
        "auto" to "Zefir/1.1.5 (iPhone; iOS)",
        "happ" to "Happ/3.15.0",
        "v2raytun" to "v2rayTun/4.7.0",
        "streisand" to "Streisand/2.0",
        "clash" to "clash-verge/v2.0.0",
    )

    suspend fun fetch(
        url: String,
        clientKind: String? = null,
        customClient: String? = null,
    ): SubscriptionFetch {
        val ua = when {
            !customClient.isNullOrBlank() -> customClient
            clientKind != null -> userAgents.firstOrNull { it.first == clientKind }?.second
                ?: userAgents.first().second
            else -> userAgents.first().second
        }
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, ua)
            header(HttpHeaders.Accept, "*/*")
        }
        if (!response.status.isSuccess()) {
            throw ImportException(if (response.status.value in 400..499) ImportError.Blocked else ImportError.Network)
        }
        val body = response.bodyAsText()
        val info = parseInfo(response.headers["subscription-userinfo"], response.headers["profile-web-page-url"])
        return SubscriptionFetch(body, info, ua)
    }

    fun parseInfo(userInfo: String?, webUrl: String?): SubscriptionInfo {
        var used = 0L
        var total = 0L
        var expire = 0L
        userInfo?.split(';')?.forEach { part ->
            val key = part.substringBefore('=').trim()
            val value = part.substringAfter('=', "").trim().toLongOrNull() ?: return@forEach
            when (key) {
                "upload", "used" -> used += value
                "download" -> Unit
                "total" -> total = value
                "expire" -> expire = value
            }
        }
        return SubscriptionInfo(used = used, total = total, expire = expire, webUrl = webUrl.orEmpty())
    }
}
