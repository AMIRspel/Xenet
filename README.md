# Xenet

**An Android VPN client for managing and testing Xray-based connections.**

Xenet brings subscriptions, individual configurations, connection controls, and server testing together in a clean Android interface.

[Download Xenet](https://github.com/AMIRspel/Xenet/releases/tag/v1.0) · [Telegram channel](https://t.me/XenetVPN) · [Support](https://t.me/XenetSup) · [Telegram bot](https://t.me/XenetBot)

---

## Download

Get Xenet from the [Releases page](https://github.com/AMIRspel/Xenet/releases/tag/v1.0).

| APK | Device |
| --- | --- |
| **Universal** | Recommended if you do not know your phone's architecture |
| **ARM64** | 64-bit ARM devices |
| **ARMv7** | 32-bit ARM devices |

Xenet targets Android 5.0 (API 21) and later. The project owner reports successful hands-on testing; compatibility can vary between devices and networks. The release includes `SHA256SUMS.txt` for checking downloaded files.

## Features

- **Flexible imports:** add supported configuration links, outbound JSON, subscriptions, or QR codes.
- **Subscription updates:** see when an update is running, succeeds, or fails. Failed downloads or parsing do not replace the existing group.
- **Real Test:** measure a download through each configuration and rank successful results within their group.
- **App routing:** choose whether all apps, selected apps, or all except selected apps use the VPN.
- **Connection controls:** adjust supported network and recovery settings, with light and dark themes.

### How Real Test works

Real Test checks configurations one at a time using an HTTPS download from Cloudflare through each server. Each response-body sample is limited to 2 MiB or 3 seconds, plus network overhead. Connection setup and slow servers can take longer. Cloudflare sees the server's exit IP, and results vary with the network and server; they are not a guarantee of future speed.

## Supported configurations

Xenet uses a third-party Xray-based engine and supports VMess, VLESS, Trojan, Shadowsocks, SOCKS and HTTP(S) links, plus supported Xray outbound JSON. Support depends on the configuration and transport. Hysteria2, TUIC, SSR and Shadowsocks plugins are not supported by the bundled UI and engine.

## Build from source

Requirements: JDK 21, Android SDK Platform 35, Build Tools 35.0.0, and internet access for Gradle and Maven dependencies. Set `ANDROID_HOME` or use `local.properties`, then run:

```sh
./gradlew :app:assembleDebug :app:testReleaseUnitTest :app:lintRelease
```

Debug builds use your own Android debug key and cannot update an APK signed with the project owner's key. Release builds without a local signing key are unsigned. Signing credentials are not included in this repository.

The application ID is `app.xenet.client.v1` and the current version is `1.0` (version code `101`). The repository includes the prebuilt AndroidLibXrayLite AAR, native tun2socks libraries, and an Xray core source snapshot under `third_party/archives/`. Go dependencies are not fully vendored, and reproduction of the bundled native binaries has not been verified.

## Testing and compatibility

The local release build passed **135 JVM unit tests**; lint reported zero errors and 19 warnings. The owner has also tested the app directly. Device coverage and network conditions vary. Compatibility of the bundled native engine with 16 KiB memory page devices remains unresolved, and no independent security or DNS/IPv6 leak audit has been completed.

APK signing certificate SHA-256:

```text
86a5ba6579403cce23120c28f9243fcd081f7a41af9ca7c78f6cdf6de30d8500
```

## Contact

- Telegram channel: [@XenetVPN](https://t.me/XenetVPN)
- Support: [@XenetSup](https://t.me/XenetSup)
- Bot: [@XenetBot](https://t.me/XenetBot)

## License

The application is distributed under [GPL-3.0](LICENSE). Third-party components retain their respective licenses; see the notices in `app/src/main/assets/licenses/` and `third_party/`.
