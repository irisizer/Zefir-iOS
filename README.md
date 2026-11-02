# Zefir for iOS

Клиент подписок и share-ссылок (vless / vmess / trojan / shadowsocks / hysteria2 / wireguard,
base64- и Clash-подписки) на Compose Multiplatform — один и тот же UI-код на iOS и десктопе.

Порт Zefir 1.1.5-beta (dev.rooni.zefir), исходный APK декомпилирован jadx + apktool.

## Что внутри

- `shared/` — весь интерфейс и логика: тема (Material 3 Expressive + динамические палитры),
  главный экран с настраиваемыми виджетами, серверы (список/сетка, сортировка, пинги),
  подписки и импорт ссылок, настройки (подключение, оформление, DNS, правила,
  автоправила, уведомления, простой, диагностика, журнал), онбординг.
- `iosApp/` — SwiftUI-обёртка, Info.plist, XcodeGen-проект, иконка из APK.
- `desktopApp/` — десктоп-запуск для локальной проверки UI под Xvfb.
- `.github/workflows/build.yml` — сборка unsigned `.ipa` на macos-26.

## Сборка

```bash
./gradlew :shared:compileKotlinDesktop      # проверка общего кода
./gradlew :desktopApp:run                   # окно приложения (нужен полный JDK, не headless)
```

`.ipa` собирается в CI: `Build iOS IPA` → артефакт `Zefir-ipa`.

## Ограничение iOS

Туннель живёт в `NEPacketTunnelProvider` и требует entitlement
`com.apple.developer.network-extension`, который не выдаётся бесплатным Apple ID
и не активируется в unsigned `.ipa`. Приложение работает как клиент-конфигуратор:
подписки, серверы, правила, пинги, тема, настройки — всё живое; кнопка подключения
честно сообщает, что системный VPN недоступен, если туннель не подключён из Swift
(`TunnelBridge.handler`).
