package dev.rooni.zefir.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine { config { followRedirects(true) } }
}
