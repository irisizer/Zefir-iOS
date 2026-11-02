package dev.rooni.zefir.app

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient

enum class ImportError { Empty, Network, Unsupported, NoServers, Blocked }

class ImportException(val error: ImportError) : Exception(error.name)
