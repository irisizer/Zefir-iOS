import Foundation
import Libbox
import Network
import NetworkExtension
import os.log

/// Реализация LibboxPlatformInterface для NEPacketTunnelProvider.
/// openTun переводит опции sing-box в NEPacketTunnelNetworkSettings
/// и отдаёт fd системного utun (как в sing-box-for-apple / wireguard-apple).
final class ExtensionPlatformInterface: NSObject, LibboxPlatformInterface {

    private let log = Logger(subsystem: "zefir.tunnel", category: "platform")
    private weak var provider: NEPacketTunnelProvider?
    private var pathMonitor: NWPathMonitorInstance?

    init(provider: NEPacketTunnelProvider) {
        self.provider = provider
        super.init()
    }

    // MARK: - TUN

    func openTun(_ options: LibboxTunOptions?) throws -> Int32 {
        guard let options, let provider else {
            throw NSError(domain: "zefir.tunnel", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "openTun без опций",
            ])
        }

        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "127.0.0.1")
        settings.mtu = NSNumber(value: options.mtu)

        func collect(_ iterator: LibboxRoutePrefixIterator?) -> [(address: String, prefix: Int32, mask: String)] {
            var result: [(String, Int32, String)] = []
            guard let iterator else { return result }
            while iterator.hasNext() {
                guard let prefix = iterator.next() else { break }
                result.append((prefix.address, prefix.prefix, prefix.mask))
            }
            return result
        }

        let ipv4 = NEIPv4Settings(addresses: [], subnetMasks: [])
        var ipv6Present = false
        let ipv6 = NEIPv6Settings(addresses: [], networkPrefixLengths: [])

        for item in collect(options.inet4Address) {
            ipv4.addresses.append(item.address)
            ipv4.subnetMasks.append(item.mask)
        }
        for item in collect(options.inet6Address) {
            ipv6.addresses.append(item.address)
            ipv6.networkPrefixLengths.append(NSNumber(value: item.prefix))
            ipv6Present = true
        }

        // Маршруты: если sing-box не дал свой список — весь трафик в туннель.
        let route4 = collect(options.inet4RouteAddress)
        let route6 = collect(options.inet6RouteAddress)
        if route4.isEmpty {
            ipv4.includedRoutes = [NEIPv4Route.defaultRoute()]
        } else {
            ipv4.includedRoutes = route4.map { NEIPv4Route(destinationAddress: $0.address, subnetMask: $0.mask) }
        }
        if ipv6Present {
            if route6.isEmpty {
                ipv6.includedRoutes = [NEIPv6Route.defaultRoute()]
            } else {
                ipv6.includedRoutes = route6.map {
                    NEIPv6Route(destinationAddress: $0.address, networkPrefixLength: NSNumber(value: $0.prefix))
                }
            }
        }

        settings.ipv4Settings = ipv4
        if ipv6Present { settings.ipv6Settings = ipv6 }

        // DNS из опций туннеля (адрес идёт следом за адресом интерфейса)
        if let box = try? options.dNSServerAddress(), let server = box?.value, !server.isEmpty {
            let dns = NEDNSSettings(servers: [server])
            dns.matchDomains = [""] // перехватываем весь DNS
            settings.dnsSettings = dns
        }

        let semaphore = DispatchSemaphore(value: 0)
        var setupError: Error?
        provider.setTunnelNetworkSettings(settings) { error in
            setupError = error
            semaphore.signal()
        }
        semaphore.wait()

        if let setupError {
            throw setupError
        }

        let fd = LibboxGetTunnelFileDescriptor()
        if fd < 0 {
            throw NSError(domain: "zefir.tunnel", code: 2, userInfo: [
                NSLocalizedDescriptionKey: "utun fd не найден в процессе расширения",
            ])
        }
        return fd
    }

    // MARK: - окружение

    func usePlatformAutoDetectInterfaceControl() -> Bool { false }

    func autoDetectInterfaceControl(_ fd: Int32) throws {
        // Android protect-сокета, на iOS не нужен
    }

    func underNetworkExtension() -> Bool { true }

    func includeAllNetworks() -> Bool { false }

    func useProcFS() -> Bool { false }

    func writeLog(_ message: String) {
        log.info("\(message, privacy: .public)")
    }

    func clearDNSCache() {}

    // MARK: - интерфейсы и монитор сети

    func getInterfaces() throws -> LibboxNetworkInterfaceIterator {
        NetworkInterfaceIterator(interfaces: SystemInterfaces.current())
    }

    func startDefaultInterfaceMonitor(_ listener: LibboxInterfaceUpdateListener?) throws {
        let monitor = NWPathMonitorInstance(listener: listener)
        monitor.start()
        pathMonitor = monitor
    }

    func closeDefaultInterfaceMonitor(_ listener: LibboxInterfaceUpdateListener?) throws {
        pathMonitor?.stop()
        pathMonitor = nil
    }

    // MARK: - заглушки

    func findConnectionOwner(
        _ ipProtocol: Int32,
        sourceAddress: String,
        sourcePort: Int32,
        destinationAddress: String,
        destinationPort: Int32
    ) throws -> Int32 {
        throw NSError(domain: "zefir.tunnel", code: 3, userInfo: [
            NSLocalizedDescriptionKey: "findConnectionOwner недоступен на iOS",
        ])
    }

    func packageNameByUid(_ uid: Int32) throws -> String {
        throw NSError(domain: "zefir.tunnel", code: 4)
    }

    func uidByPackageName(_ packageName: String) throws -> Int32 {
        throw NSError(domain: "zefir.tunnel", code: 5)
    }

    func readWIFIState() -> LibboxWIFIState? { nil }

    func sendNotification(_ notification: LibboxNotification?) throws {}
}

// MARK: - итераторы

final class NetworkInterfaceIterator: NSObject, LibboxNetworkInterfaceIterator {
    private let interfaces: [LibboxNetworkInterface]
    private var index = 0

    init(interfaces: [LibboxNetworkInterface]) {
        self.interfaces = interfaces
    }

    func len() -> Int32 { Int32(interfaces.count) }

    func hasNext() -> Bool { index < interfaces.count }

    func next() -> LibboxNetworkInterface? {
        defer { index += 1 }
        return index < interfaces.count ? interfaces[index] : nil
    }
}

final class StringArrayIterator: NSObject, LibboxStringIterator {
    private let values: [String]
    private var index = 0

    init(values: [String]) {
        self.values = values
    }

    func len() -> Int32 { Int32(values.count) }

    func hasNext() -> Bool { index < values.count }

    func next() -> String {
        defer { index += 1 }
        return index < values.count ? values[index] : ""
    }
}

// MARK: - системные интерфейсы через getifaddrs

enum SystemInterfaces {

    static func current() -> [LibboxNetworkInterface] {
        var result: [LibboxNetworkInterface] = []
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else { return result }
        defer { freeifaddrs(ifaddr) }

        var seen = Set<String>()
        var pointer: UnsafeMutablePointer<ifaddrs>? = first
        while let current = pointer {
            let entry = current.pointee
            let name = String(cString: entry.ifa_name)
            pointer = entry.ifa_next
            if !seen.insert(name).inserted { continue }

            let item = LibboxNetworkInterface()
            item.index = Int32(if_nametoindex(name))
            item.name = name
            item.mtu = 1500
            item.flags = Int32(entry.ifa_flags)
            item.type = 0 // Other
            item.addresses = StringArrayIterator(values: [])
            item.dnsServer = StringArrayIterator(values: [])
            item.metered = false
            result.append(item)
        }
        return result
    }
}

// MARK: - монитор сети через NWPathMonitor

final class NWPathMonitorInstance {
    private let monitor = NWPathMonitor()
    private let listener: LibboxInterfaceUpdateListener?
    private let queue = DispatchQueue(label: "zefir.tunnel.monitor")

    init(listener: LibboxInterfaceUpdateListener?) {
        self.listener = listener
    }

    func start() {
        monitor.pathUpdateHandler = { [weak self] path in
            guard let self, let listener = self.listener else { return }
            let name = Self.interfaceName(of: path)
            listener.updateDefaultInterface(name, index: 0, isExpensive: path.isExpensive, isConstrained: false)
        }
        monitor.start(queue: queue)
    }

    func stop() {
        monitor.cancel()
    }

    private static func interfaceName(of path: NWPath) -> String {
        if let interface = path.availableInterfaces.first(where: { $0.type == .wifi }) {
            return interface.name
        }
        if let interface = path.availableInterfaces.first(where: { $0.type == .cellular }) {
            return interface.name
        }
        return path.availableInterfaces.first?.name ?? "en0"
    }
}
