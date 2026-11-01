package dev.rooni.zefir.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Состояние туннеля — как в оригинале (model/TunnelState). */
sealed interface TunnelState {
    data object Off : TunnelState
    data object Starting : TunnelState
    data object Stopping : TunnelState
    data class On(val since: Long, val traffic: Traffic) : TunnelState
    data class Failed(val reason: String?) : TunnelState
}

data class Traffic(val up: Long = 0, val down: Long = 0)

enum class RoutingMode { Smart, Global, Direct }

enum class ThemeMode { System, Light, Dark }

enum class ServerLayout { List, Grid }

enum class ServerDensity { Compact, Normal, Roomy }

enum class ServerSort { Default, Name, Ping }

enum class PingMethod { Tcp, Url }

enum class DnsMode { FromProfile, Secure, Custom }

enum class SplitMode { Off, Exclude, Only }

/** Один сервер из подписки/ссылки. */
@Serializable
data class ProxyNode(
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
    val country: String? = null,
    val flag: String? = null,
    val link: String,
    val latency: Int? = null,
    val group: String? = null,
)

/** Подписка (Profile в оригинале). */
@Serializable
data class Profile(
    val id: String,
    val name: String,
    val url: String = "",
    val updatedAt: Long = 0,
    val used: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val servers: Int = 0,
    val pinned: Set<String> = emptySet(),
    val updateHours: Int? = null,
    val supportUrl: String? = null,
    val webUrl: String? = null,
    val announce: String? = null,
    val client: String? = null,
    val customClient: String? = null,
    val nodes: List<ProxyNode> = emptyList(),
) {
    val hasTrafficLimit: Boolean get() = total > 0
    val hasTerm: Boolean get() = expire > 0
}

/** Настройки — подмножество AppSettings оригинала, нужное iOS-клиенту. */
@Serializable
data class AppSettings(
    val onboarded: Boolean = false,
    val activeProfile: String? = null,
    val activeServer: String? = null,
    val mode: RoutingMode = RoutingMode.Smart,
    val splitMode: SplitMode = SplitMode.Off,
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val accent: Long = 4281294160,
    val palette: Int = -1,
    val haptics: Boolean = true,
    val detailedErrors: Boolean = false,
    val autoSwitch: Boolean = false,
    val serverLayout: ServerLayout = ServerLayout.List,
    val serverDensity: ServerDensity = ServerDensity.Normal,
    val serverSort: ServerSort = ServerSort.Default,
    val serverSortDesc: Boolean = false,
    val pingMethod: PingMethod = PingMethod.Url,
    val widgets: List<String> = listOf("Server", "Subscription"),
    val autoUpdateHours: Int = 24,
    val connectOnStart: Boolean = false,
    val reminders: Boolean = true,
    val blockAds: Boolean = false,
    val fakeIp: Boolean = true,
    val sniffer: Boolean = false,
    val ipv6: Boolean = false,
    val dnsMode: DnsMode = DnsMode.FromProfile,
    val customDns: String = "",
    val mtu: Int = 1500,
    val testUrl: String = "https://www.gstatic.com/generate_204",
    val logLevel: String = "info",
)

enum class Tab { Home, Servers, Settings }