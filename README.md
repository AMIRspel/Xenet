# Xenet

Xenet is an experimental Android VPN client with an English interface and support for Xray-based configurations. This repository publishes the application source and bundled dependencies for inspection and development. The app uses a third-party Xray-based native engine; it is **not** a new protocol engine written from scratch.

> **Status: experimental.** This build has not been tested on a physical Android device, a user's live subscription, or across device vendors. It has not had a security, DNS/IPv6 leak, or privacy audit. The bundled native engine has 4 KiB ELF alignment; compatibility with 16 KiB page-size devices is unresolved. Do not treat a successful build or a public repository as a security guarantee.

## Downloads

Experimental Android APKs, when available, are listed on the [Releases page](https://github.com/AMIRspel/Xenet/releases). Read the warnings and release notes before installing.

## Features

- Import supported config links, outbound JSON, subscriptions and QR codes.
- Subscription updates report in-progress, success, and failure; failed downloads or parsing do not commit a replacement.
- **Real Test** samples a verified HTTPS download through one configuration at a time and ranks successful results within their group. The sample uses up to 2 MiB or 3 seconds of response body per server, plus overhead. Slow servers and connection setup can take longer. Results vary by network and server; Cloudflare sees the server's exit IP.
- Per-app routing choices, connection settings, and reconnect controls. Changes to routing and connection settings require Stop, then Connect.

Not all protocol features or devices are supported. In particular, the bundled engine does not support Hysteria2, TUIC, SSR or Shadowsocks plugins through this UI. See source code and included licenses for details.

## Build

Requirements: JDK 21, Android SDK Platform 35 and Build Tools 35.0.0, plus internet access to resolve Gradle/Maven dependencies. Set `ANDROID_HOME` or `local.properties` and run:

```sh
./gradlew :app:assembleDebug :app:testReleaseUnitTest :app:lintRelease
```

The debug APK is signed with your own Android debug key. It cannot update the owner's signed APK. Release builds without a local signing key are unsigned. Signing credentials are **not** in this repository and must never be committed. The source configuration supports a local `signing/` directory, which `.gitignore` excludes.

Version name is `1.0`, version code `101`, and the application ID for these builds is `app.xenet.client.v1`. The project includes the prebuilt AndroidLibXrayLite AAR and native tun2socks libraries. A snapshot of Xray core source is included in `third_party/archives/`; Go dependencies are not fully vendored. The `third_party/AndroidLibXrayLite/go.mod` file records pinned dependencies. Reproducing the included native binaries from source has not been verified. See `third_party/` and the license files for upstream components and license notices.

## Testing

The most recent local release build and lint completed with **135 JVM unit tests passed**, zero lint errors and 19 warnings. JVM tests are not Android device tests. The subscription tests cover response parsing, fetch failure and commit failure with test fixtures; they do not validate a real provider link or Android UI lifecycle. SHA-256 of the app's signing certificate (not the private key):

`86a5ba6579403cce23120c28f9243fcd081f7a41af9ca7c78f6cdf6de30d8500`

## License

The application is distributed under GPL-3.0; see [LICENSE](LICENSE). Third-party components retain their own licenses; see the notices in `app/src/main/assets/licenses/` and `third_party/`.

---

## فارسی

زینت یک کلاینت آزمایشی اندروید است. تست واقعی، نمونه‌ای از دانلود را از مسیر هر کانفیگ می‌سنجد و نتیجه‌های سریع‌تر را در گروه خودشان بالاتر می‌آورد. این تست اینترنت مصرف می‌کند و تضمین سرعت آینده نیست.

سورس برای بررسی منتشر شده، اما برنامه هنوز روی گوشی واقعی و لینک ساب کاربران آزمایش نشده و ممیزی امنیتی و بررسی نشت DNS/IPv6 انجام نشده است. سازگاری هسته با دستگاه‌های ۱۶ کیلوبایتی نیز هنوز تأیید نشده است. کلید خصوصی امضا در این مخزن قرار ندارد.
