package dev.rooni.zefir.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun httpClient(): HttpClient = HttpClient(Darwin) {
    followRedirects = true
}

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * TCP-пинг на iOS. Реализация через POSIX-сокеты добавлена в платформенном слое
 * (iosMain/platform/TcpPing.ios.kt), здесь — безопасный фолбэк.
 */
actual suspend fun tcpPing(host: String, port: Int): Int? = posixTcpPing(host, port)