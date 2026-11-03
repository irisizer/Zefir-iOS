import Foundation
import Libbox
import Network
import NetworkExtension
import os.log

/// Реализация LibboxPlatformInterface для NEPacketTunnelProvider.
/// openTun переводит опции sing-box в NEPacketTunnelNetworkSettings
/// и отдаёт fd системного utun (как в sing-box-for-apple / wireguard-apple).
final class ExtensionPlatformInterface: NSObject, LibboxPlatformInterfaceProtocol {

    private let log = Logger(subsystem: "zefir.tunnel", category: "platform")
    private weak var provider: NEPacketTunnelProvider?
    private var pathMonitor: NWPathMonitor?
    private let monitorQueue = DispatchQueue(label: "zefir.tunnel.monitor")

    init(provider: NEPacketTunnelProvider) {
        self.provider = provider
        super.init()
    }

    private struct RouteItem {
        let address: String
        let prefix: Int32
        let mask: String
    }

    // MARK: - TUN

    func openTun(_ options: LibboxTunOptionsProtocol?, ret0_: UnsafeMutablePointer<Int32>?) throws {
        guard let options, let provider else {
            throw NSError(domain: "zefir.tunnel", code: 1, userInfo: [
                NSLocalizedDescriptionKey: "openTun без опций",
            ])
        }

        func collect(_ getter: () -> LibboxRoutePrefixIteratorProtocol?) -> [RouteItem] {
            var result: [RouteItem] = []
            guard let iterator = getter() else { return result }
            while iterator.hasNext() {
                guard let prefix = iterator.next() else { break }
                let item = RouteItem(
                    address: prefix.address as String? ?? "",
                    prefix: (prefix.prefix as Int32? ?? 0),
                    mask: prefix.mask as String? ?? ""
                )
                result.append(item)
            }
            return result
        }

        let inet4 = collect { options.getInet4Address() }
        let inet6 = collect { options.getInet6Address() }
        let route4 = collect { options.getInet4RouteAddress() }
        let route6 = collect { options.getInet6RouteAddress() }

        var v4Addresses: [String] = []
        var v4Masks: [String] = []
        for item in inet4 {
            v4Addresses.append(item.address)
            v4Masks.append(item.mask)
        }
        var v6Addresses: [String] = []
        var v6Prefixes: [NSNumber] = []
        for item in inet6 {
            v6Addresses.append(item.address)
            v6Prefixes.append(NSNumber(value: item.prefix))
        }

        let ipv4Settings: NEIPv4Settings
        if route4.isEmpty {
            ipv4Settings = NEIPv4Settings(addresses: v4Addresses, subnetMasks: v4Masks)
            ipv4Settings.includedRoutes = [NEIPv4Route.default()]
        } else {
            ipv4Settings = NEIPv4Settings(addresses: v4Addresses, subnetMasks: v4Masks)
            ipv4Settings.includedRoutes = route4.map {
                NEIPv4Route(destinationAddress: $0.address, subnetMask: $0.mask)
            }
        }

        var ipv6Settings: NEIPv6Settings? = nil
        if !v6Addresses.isEmpty {
            let settings = NEIPv6Settings(addresses: v6Addresses, networkPrefixLengths: v6Prefixes)
            if route6.isEmpty {
                settings.includedRoutes = [NEIPv6Route.default()]
            } else {
                settings.includedRoutes = route6.map {
                    NEIPv6Route(destinationAddress: $0.address, networkPrefixLength: NSNumber(value: $0.prefix))
                }
            }
            ipv6Settings = settings
        }

        let settings = NEPacketTunnelNetworkSettings(tunnelRemoteAddress: "127.0.0.1")
        settings.mtu = NSNumber(value: options.getMTU())
        settings.ipv4Settings = ipv4Settings
        if let ipv6Settings {
            settings.ipv6Settings = ipv6Settings
        }

        // DNS из опций туннеля (адрес идёт следом за адресом интерфейса)
        if let box = try? options.getDNSServerAddress() {
            let server = box.value as String? ?? ""
            if !server.isEmpty {
                let dns = NEDNSSettings(servers: [server])
                dns.matchDomains = [""] // перехватываем весь DNS
                settings.dnsSettings = dns
            }
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

        // fd системного utun, к которому привязан packetFlow
        let fd = LibboxGetTunnelFileDescriptor()
        if fd < 0 {
            throw NSError(domain: "zefir.tunnel", code: 2, userInfo: [
                NSLocalizedDescriptionKey: "utun fd не найден в процессе расширения",
            ])
        }
        ret0_?.pointee = fd
    }

    // MARK: - окружение

    func usePlatformAutoDetectControl() -> Bool { false }

    func autoDetectControl(_ fd: Int32) throws {
        // Android protect-сокета, на iOS не нужен
    }

    func underNetworkExtension() -> Bool { true }

    func includeAllNetworks() -> Bool { false }

    func useProcFS() -> Bool { false }

    func writeLog(_ message: String?) {
        log.info("\(message ?? "", privacy: .public)")
    }

    func clearDNSCache() {}

    // MARK: - интерфейсы и монитор сети

    func getInterfaces() throws -> LibboxNetworkInterfaceIteratorProtocol {
        NetworkInterfaceArray(SystemInterfaces.current())
    }

    func startDefaultInterfaceMonitor(_ listener: LibboxInterfaceUpdateListenerProtocol?) throws {
        guard let listener else { return }
        let monitor = NWPathMonitor()
        pathMonitor = monitor
        let semaphore = DispatchSemaphore(value: 0)
        monitor.pathUpdateHandler = { [weak self] path in
            self?.notify(listener, path: path)
            semaphore.signal()
        }
        monitor.start(queue: monitorQueue)
        semaphore.wait()
    }

    func closeDefaultInterfaceMonitor(_ listener: LibboxInterfaceUpdateListenerProtocol?) throws {
        pathMonitor?.cancel()
        pathMonitor = nil
    }

    private func notify(_ listener: LibboxInterfaceUpdateListenerProtocol, path: Network.NWPath) {
        let name: String
        if let wifi = path.availableInterfaces.first(where: { $0.type == .wifi }) {
            name = wifi.name
        } else if let cellular = path.availableInterfaces.first(where: { $0.type == .cellular }) {
            name = cellular.name
        } else {
            name = path.availableInterfaces.first?.name ?? "en0"
        }
        listener.updateDefaultInterface(name, interfaceIndex: 0, isExpensive: path.isExpensive, isConstrained: false)
    }

    // MARK: - заглушки

    func findConnectionOwner(
        _ ipProtocol: Int32,
        sourceAddress: String?,
        sourcePort: Int32,
        destinationAddress: String?,
        destinationPort: Int32,
        ret0_: UnsafeMutablePointer<Int32>?
    ) throws {
        ret0_?.pointee = -1
    }

    func packageName(byUid uid: Int32, error: NSErrorPointer) -> String {
        _ = error
        return ""
    }

    func uid(byPackageName packageName: String?, ret0_: UnsafeMutablePointer<Int32>?) throws {
        ret0_?.pointee = -1
    }

    func readWIFIState() -> LibboxWIFIState? { nil }

    func send(_ notification: LibboxNotification?) throws {}
}

// MARK: - итератор интерфейсов

final class NetworkInterfaceArray: NSObject, LibboxNetworkInterfaceIteratorProtocol {
    private var iterator: IndexingIterator<[LibboxNetworkInterface]>
    private var nextValue: LibboxNetworkInterface?

    init(_ array: [LibboxNetworkInterface]) {
        iterator = array.makeIterator()
    }

    func hasNext() -> Bool {
        nextValue = iterator.next()
        return nextValue != nil
    }

    func next() -> LibboxNetworkInterface? { nextValue }
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
            item.name = name
            item.index = Int32(if_nametoindex(name))
            item.mtu = 1500
            item.type = LibboxInterfaceTypeOther
            result.append(item)
        }
        return result
    }
}
