package dev.rooni.zefir.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun httpClient(): HttpClient = HttpClient(OkHttp) {
    followRedirects = true
}

actual fun nowMillis(): Long = System.currentTimeMillis()

actual suspend fun tcpPing(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
    val started = System.currentTimeMillis()
    runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2500)
        }
    }.map { (System.currentTimeMillis() - started).toInt() }.getOrNull()
}