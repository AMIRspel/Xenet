# Dependency provenance — not original Xenet network engines

New app Java code: GPL-3.0-or-later; root LICENSE. No v2rayNG application Java/Kotlin, screens, resources or persistence libraries are compiled. Gradle wrapper is shared build infrastructure, not application code.

Bundled unmodified libv2ray.aar was retained from the prior supplied engine artifacts, not rebuilt here. AndroidLibXrayLite source: https://github.com/2dust/AndroidLibXrayLite at fe7e12eb08b45cab694799bcc77edfdce66bcae0 (LGPL-3.0); go.mod pins github.com/xtls/xray-core v1.250726.0 (MPL-2.0). Wrapper source and Go module manifests are included, and Xray source archive is under archives/. Transitive Go modules are identified by go.mod/go.sum and retrievable using Go modules; not all dependency sources are vendored. Historical binary/source bit-reproducibility is NOT attested.

Native tun2socks executable libraries: badvpn e68480088a48c8930d29f54e30e9d6deaaf32c47 (BSD-3-Clause), libancillary 232d69a5ebb4461b572bd3f3b97088091e01c243 (license headers retained). Source trees and notices included. Original native artifacts were reused; no native protocol engine is claimed as newly authored. Hysteria source/library is not included or used in this rewrite. Source-only historical geoip/geosite data files were removed because the new routing config does not use them; none is packaged in the application.

OkHttp 4.12.0 / Okio and Kotlin stdlib transitive dependencies: Square / JetBrains, Apache-2.0. ZXing core 3.5.3: Apache-2.0. Resolved from Maven Central by Gradle. JVM tests additionally use JUnit 4.13.2 (EPL-1.0), org.json 20240303 (public-domain notice), Hamcrest (BSD). These test libraries are not application dependencies.

Vazirmatn and Manrope fonts: SIL OFL, respective OFL files in app/src/main/assets/fonts. Lucide icon paths: Lucide contributors, ISC, license included. Xenet logo is newly drawn code-native vector/PNG; it is not the Happ logo or copied Happ artwork.

For publication, audit the complete resolved dependency/license graph, update vulnerable/outdated components, build native engines reproducibly including 16KiB alignment, and preserve all required source/notice and replacement/relinking rights. This package is an engineering alpha, not a compliance certification.

## Font update for UI 0.8
Vazirmatn[wght].ttf and Manrope[wght].ttf fetched from the corresponding google/fonts ofl folders and instantiated to 400 and 650 with fontTools. OFL notices are retained under assets/fonts. Static TTF hashes are included in UI-FONTS.json. This weight instantiation does not alter the glyph design.

## 0.9 UI assets
Only Manrope fonts are packaged (400/650 body weights and800 wordmark; retained historical750 face is also Manrope). Persian font assets and language UI were removed. Newly authored heavier wireless/X logo supplied as SVG/PNG, including launcher variants. Icons remain ISC Lucide paths; Android vector numeric syntax normalized without changing their geometry.
