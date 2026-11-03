import Foundation
import NetworkExtension
import Shared

/// Управляет VPN-конфигурацией (NETunnelProviderManager) и мостит её
/// в Kotlin через протокол TunnelHandler.
final class TunnelManager: NSObject, TunnelHandler {

    static let shared = TunnelManager()

    private enum TunnelConstants {
        static let appGroup = "group.833d43e5a1beb974.1"
    }

    private var manager: NETunnelProviderManager?
    private var observer: NSObjectProtocol?
    private var lastStatus: NEVPNStatus = .invalid

    @discardableResult
    func start(config: String) -> Any? {
        guard
            let container = FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: TunnelConstants.appGroup)
        else { return }
        try? config.write(
            to: container.appendingPathComponent("singbox.json"),
            atomically: true,
            encoding: .utf8
        )
        Task { await installAndStart() }
        return nil
    }

    @discardableResult
    func stop() -> Any? {
        let session = manager?.connection as? NETunnelProviderSession
            ?? (try? NETunnelProviderManager.loadAllFromPreferencesAndWait().first)?.connection as? NETunnelProviderSession
        session?.stopVPNTunnel()
        return nil
    }

    func status() -> Int32 {
        let vpnStatus = currentStatus()
        switch vpnStatus {
        case .connected, .reasserting: return 2
        case .connecting: return 1
        case .disconnecting: return 3
        default: return 0
        }
    }

    func traffic() -> Traffic {
        // Трафик в v1 не считается: счётчик сеанса ведёт само приложение.
        Traffic(up: 0, down: 0, upSpeed: 0, downSpeed: 0)
    }

    // MARK: - установка и запуск

    private func installAndStart() async {
        do {
            let managers = try await NETunnelProviderManager.loadAllFromPreferences()
            let tunnel = managers.first { manager in
                (manager.protocolConfiguration as? NETunnelProviderProtocol)?
                    .providerBundleIdentifier?.hasSuffix(".ZefirTunnel") == true
            } ?? NETunnelProviderManager()

            let proto = (tunnel.protocolConfiguration as? NETunnelProviderProtocol)
                ?? NETunnelProviderProtocol()
            proto.providerBundleIdentifier = (Bundle.main.bundleIdentifier ?? "") + ".ZefirTunnel"
            proto.serverAddress = "Zefir"
            tunnel.protocolConfiguration = proto
            tunnel.localizedDescription = "Zefir"
            tunnel.isEnabled = true

            try await tunnel.saveToPreferences()
            try await tunnel.loadFromPreferences()
            manager = tunnel
            observeStatus(tunnel)

            guard let session = tunnel.connection as? NETunnelProviderSession else { return }
            try session.startVPNTunnel()
        } catch {
            NSLog("Zefir tunnel install/start failed: \(error.localizedDescription)")
        }
    }

    private func currentStatus() -> NEVPNStatus {
        if let manager {
            return manager.connection.status
        }
        if
            let managers = try? NETunnelProviderManager.loadAllFromPreferencesAndWait(),
            let tunnel = managers.first(where: { manager in
                (manager.protocolConfiguration as? NETunnelProviderProtocol)?
                    .providerBundleIdentifier?.hasSuffix(".ZefirTunnel") == true
            })
        {
            manager = tunnel
            observeStatus(tunnel)
            return tunnel.connection.status
        }
        return .invalid
    }

    private func observeStatus(_ tunnel: NETunnelProviderManager) {
        guard observer == nil else { return }
        observer = NotificationCenter.default.addObserver(
            forName: .NEVPNStatusDidChange,
            object: tunnel.connection,
            queue: .main
        ) { [weak self] _ in
            self?.lastStatus = tunnel.connection.status
        }
    }
}

private extension NETunnelProviderManager {

    static func loadAllFromPreferencesAndWait() throws -> [NETunnelProviderManager] {
        var result: [NETunnelProviderManager] = []
        var thrownError: Error?
        let semaphore = DispatchSemaphore(value: 0)
        NETunnelProviderManager.loadAllFromPreferences { managers, error in
            result = managers ?? []
            thrownError = error
            semaphore.signal()
        }
        semaphore.wait()
        if let thrownError { throw thrownError }
        return result
    }
}
