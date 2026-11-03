package dev.rooni.zefir.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- перечисления (как в APK 1.1.5) ----------

enum class ThemeMode { System, Light, Dark }

enum class ServerSort { Default, Name, Ping }

enum class ServerLayout { List, Grid }

enum class ServerDensity(val cell: Int, val rowHeight: Int) {
    Roomy(84, 76),
    Normal(72, 64),
    Compact(60, 52),
}

enum class SplitMode { Off, Only, Exclude }

enum class RoutingMode(val core: String) {
    Smart("rule"),
    Global("global"),
    Direct("direct"),
}

enum class DnsMode { FromProfile, Secure, Custom }

enum class PingMethod { Tcp, Url }

enum class RuleAction { Proxy, Direct, Block }

enum class HomeWidget {
    Traffic, Server, Subscription, Speed, Ip, Mode, SplitApps;

    companion object {
        val defaultOrder = listOf(Server, Subscription)
    }
}

enum class Tab(val id: String) {
    Home("tab_home"),
    Servers("tab_servers"),
    Settings("tab_settings"),
}

// ---------- узлы и группы ----------

@Serializable
data class ProxyNode(
    val name: String,
    val type: String,
    val server: String = "",
    val port: Int = 0,
    val link: String = "",
    val country: String? = null,
    val flag: String? = null,
    /** группа/сервис из подписки, если сервер привязан к отдельному сервису */
    val group: String? = null,
) {
    val key: String get() = "$name|$server|$port"
}

@Serializable
data class ProxyGroup(
    val name: String,
    val type: String,
    val now: String = "",
    val members: List<String> = emptyList(),
)

@Serializable
data class ProxySnapshot(
    val nodes: Map<String, ProxyNode> = emptyMap(),
    val groups: List<ProxyGroup> = emptyList(),
) {
    fun all(): List<ProxyNode> = nodes.values.toList()
}

@Serializable
data class SubscriptionRouting(
    val rules: List<String> = emptyList(),
    val directDomains: List<String> = emptyList(),
)

@Serializable
data class SubscriptionInfo(
    val used: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val supportUrl: String = "",
    val webUrl: String = "",
    val announce: String = "",
)

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val url: String = "",
    val updatedAt: Long = 0,
    val servers: Int = 0,
    val nodes: List<ProxyNode> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val notes: Map<String, String> = emptyMap(),
    val links: Map<String, String> = emptyMap(),
    /** выбранный сервер для конкретной группы (сервиса) */
    val selected: Map<String, String> = emptyMap(),
    val used: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val supportUrl: String = "",
    val webUrl: String = "",
    val announce: String = "",
    val client: String = "auto",
    val customClient: String = "",
    val routing: SubscriptionRouting = SubscriptionRouting(),
    val routingEnabled: Boolean = false,
    val updateHours: Int = 0,
    val isMerged: Boolean = false,
) {
    val hasTraffic: Boolean get() = total > 0
    val hasTerm: Boolean get() = expire > 0
}

@Serializable
data class UserRule(val value: String, val action: RuleAction)

@Serializable
data class AppPack(val id: String, val titleKey: String, val patterns: List<String>)

// ---------- состояние туннеля ----------

sealed interface TunnelState {
    data object Off : TunnelState
    data object Starting : TunnelState
    data object On : TunnelState
    data object Stopping : TunnelState
    data class Failed(val message: String) : TunnelState
}

data class TunnelOptions(
    val mode: RoutingMode = RoutingMode.Smart,
    val ipv6: Boolean = false,
    val mtu: Int = 9000,
    val splitMode: SplitMode = SplitMode.Off,
    val only: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
    val allowBypass: Boolean = false,
    val dnsHijackAll: Boolean = true,
)

// ---------- трафик ----------

data class Traffic(val up: Long = 0, val down: Long = 0, val upSpeed: Long = 0, val downSpeed: Long = 0)

data class ConnectionInfo(
    val host: String,
    val network: String,
    val rule: String,
    val chain: String,
    val up: Long,
    val down: Long,
)

// ---------- настройки ----------

@Serializable
data class AppSettings(
    val accent: Long = 0xFF2F5D50,  // дефолт из APK 1.1.5 (AccentChoices[5])
    /** -1 — один акцент, 0..5 — мультипалитра */
    val palette: Int = -1,
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val language: String = "ru",
    val haptics: Boolean = true,
    val onboarded: Boolean = false,

    val activeProfile: String? = null,
    val activeServer: String? = null,
    val autoSelect: Boolean = true,

    val mode: RoutingMode = RoutingMode.Smart,
    val dnsMode: DnsMode = DnsMode.FromProfile,
    val customDns: String = "",
    val fakeIp: Boolean = true,
    val ipv6: Boolean = false,
    val mtu: Int = 9000,
    val stack: String = "system",
    val tcpConcurrent: Boolean = true,
    val sniffer: Boolean = true,
    val testUrl: String = "https://www.gstatic.com/generate_204",
    val unifiedDelay: Boolean = false,
    val pingMethod: PingMethod = PingMethod.Url,
    val skipCertVerify: Boolean = false,
    val logLevel: String = "info",
    val blockAds: Boolean = false,
    val dnsHijackAll: Boolean = true,
    val showAdvanced: Boolean = false,
    val detailedErrors: Boolean = false,

    val splitMode: SplitMode = SplitMode.Off,
    val onlyApps: Set<String> = emptySet(),
    val excludedApps: Set<String> = emptySet(),
    val appliedPacks: Set<String> = emptySet(),

    val region: String = "",
    val regionDirect: Boolean = false,
    val rules: List<UserRule> = emptyList(),
    val rawRules: List<String> = emptyList(),

    val serverSort: ServerSort = ServerSort.Default,
    val serverSortDesc: Boolean = false,
    val serverLayout: ServerLayout = ServerLayout.List,
    val serverDensity: ServerDensity = ServerDensity.Normal,
    val railExpanded: Boolean = false,
    val widgets: List<HomeWidget> = HomeWidget.defaultOrder,  // APK: [Server, Subscription]
    val searchHistory: List<String> = emptyList(),

    val connectOnStart: Boolean = false,
    val autoSwitch: Boolean = false,
    val autoRules: Boolean = false,
    val trustedWifi: Set<String> = emptySet(),
    val autoConnectElsewhere: Boolean = false,
    val idleOff: Boolean = false,
    val idleOffMinutes: Int = 15,
    val idleNightOnly: Boolean = false,

    val notifEnabled: Boolean = true,
    val notifLiveSpeed: Boolean = true,
    val notifPauseButton: Boolean = false,
    val notifStopButton: Boolean = true,
    val battery: Boolean = true,
    val reminders: Boolean = false,
    val autoUpdate: Int = 24,

    val deviceHwid: String = "",
    val deviceModel: String = "",
    val deviceOsVersion: String = "",
) {
    val accentColor: Long get() = accent
}

@Serializable
data class Backup(
    val version: Int = 1,
    val settings: AppSettings = AppSettings(),
    val profiles: List<Profile> = emptyList(),
)
