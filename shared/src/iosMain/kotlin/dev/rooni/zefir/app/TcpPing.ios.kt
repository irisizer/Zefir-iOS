package dev.rooni.zefir.app

/**
 * TCP-пинг на iOS.
 *
 * Порт с оригинального TcpPinger (Android — java.net.Socket). На iOS TCP-коннект
 * доступен через POSIX-сокеты/Network.framework; здесь оставлен безопасный фолбэк,
 * чтобы сборка не зависела от cinterop-деталей: UI покажет «—» вместо задержки,
 * а реальный пинг выполняется через URL-проверку (settings.testUrl) в контроллере.
 */
fun posixTcpPing(host: String, port: Int): Int? = null