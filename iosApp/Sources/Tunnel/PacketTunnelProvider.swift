import Foundation
import Libbox
import NetworkExtension
import os.log

/// Туннель-расширение: запускает sing-box (libbox) поверх packetFlow.
/// Классически по образцу sing-box-for-apple: конфиг лежит в App Group,
/// openTun возвращает fd системного utun, найденного в процессе расширения.
final class PacketTunnelProvider: NEPacketTunnelProvider {

    private let log = Logger(subsystem: "zefir.tunnel", category: "packet-tunnel")
    private var service: LibboxBoxService?
    private var platformInterface: ExtensionPlatformInterface?

    override init() {
        super.init()
    }

    override func startTunnel(
        options: [String: NSObject]?,
        completionHandler: @escaping (Error?) -> Void
    ) {
        // Лимит памяти расширения на iOS ~50 МБ — просим Go поджиматься.
        LibboxSetMemoryLimit(true)

        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: TunnelConstants.appGroup)
        else {
            completionHandler(TunnelError.noAppGroup)
            return
        }
        let workingDir = container.appendingPathComponent("libbox", isDirectory: true)
        let cacheDir = container.appendingPathComponent("libbox-cache", isDirectory: true)
        try? FileManager.default.createDirectory(at: workingDir, withIntermediateDirectories: true)
        try? FileManager.default.createDirectory(at: cacheDir, withIntermediateDirectories: true)

        let configPath = container.appendingPathComponent("singbox.json")
        guard
            let config = try? String(contentsOf: configPath, encoding: .utf8),
            !config.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            completionHandler(TunnelError.noConfig)
            return
        }

        do {
            let setup = LibboxSetupOptions()
            setup.basePath = container.path
            setup.workingPath = workingDir.path
            setup.tempPath = cacheDir.path
            try LibboxSetup(setup)
        } catch {
            log.fault("libbox setup: \(error.localizedDescription, privacy: .public)")
            completionHandler(error)
            return
        }

        let interface = ExtensionPlatformInterface(provider: self)
        do {
            let service = try LibboxNewService(config, interface)
            try service.start()
            self.service = service
            self.platformInterface = interface
            completionHandler(nil)
        } catch {
            log.fault("libbox start: \(error.localizedDescription, privacy: .public)")
            writeErrorLog("\(Date()) start failed: \(error.localizedDescription)")
            cancelTunnelWithError(error)
        }
    }

    override func stopTunnel(with reason: NEProviderStopReason, completionHandler: @escaping () -> Void) {
        try? service?.close()
        service = nil
        platformInterface = nil
        completionHandler()
    }

    override func handleAppMessage(_ messageData: Data) async -> Data? {
        messageData
    }

    private func writeErrorLog(_ message: String) {
        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: TunnelConstants.appGroup)
        else { return }
        let url = container.appendingPathComponent("tunnel-error.log")
        try? (message + "\n").data(using: .utf8)?.write(to: url, options: .atomic)
    }
}

enum TunnelError: LocalizedError {
    case noAppGroup
    case noConfig

    var errorDescription: String? {
        switch self {
        case .noAppGroup: return "App Group недоступен — проверь entitlements"
        case .noConfig: return "Конфиг sing-box не найден в App Group"
        }
    }
}

enum TunnelConstants {
    static let appGroup = "group.833d43e5a1beb974.1"
    static let configKey = "zefir.singbox.config"
    static let statusKey = "zefir.tunnel.status"
}
