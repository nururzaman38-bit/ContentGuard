# App Blocker

A sideloaded, dark-theme Kotlin Android utility for personal content filtering and self-control. It has independent protection switches for DNS/VPN filtering, accessibility scanning, click interception, reels warnings, scheduling, SafeSearch/private browsing checks, and usage tracking. Protection switches lock after their first activation; **Uninstall Protection is deliberately reversible for development testing** (see the `TODO: PRODUCTION` comment).

## Build and install

Push any branch (or open a pull request). GitHub Actions in `.github/workflows/build.yml` installs JDK 17, Android SDK 35, NDK 27 and CMake, then runs `./gradlew assembleDebug`. Download `app-debug.apk` from the `app-blocker-debug` workflow artifact and sideload it on an Android 8.0+ device. This project does not require an emulator or a signing key.

The repository includes the Gradle wrapper. If a fresh checkout does not have the wrapper distribution cached, `gradlew` downloads Gradle 8.11.1 from services.gradle.org.

## Optional supplied assets

Create `app/src/main/assets/` and place:

* `blocklist.txt` — one domain per line (comments beginning with `#` are suitable for a human-maintained list).
* `nsfw_model.tflite` — an on-device TensorFlow Lite model for frame and thumbnail classification.

The app is intentionally usable without them and logs a clear missing-asset message when the production loaders are added. No user content leaves the device. The native library (`app/src/main/cpp`) supplies hash-based domain matching, keyword matching, and the JNI boundary used by the scanning service; it is built with CMake for every ABI selected by Android Gradle Plugin.

## Permissions and why

* `BIND_ACCESSIBILITY_SERVICE`: Android grants this only after the user enables the service in Accessibility settings; it scans watched app view trees and can return Home for a blocked control.
* `BIND_VPN_SERVICE`: grants the local `VpnService` tunnel used for DNS/domain filtering. The tunnel does not send data to an application server.
* `BIND_DEVICE_ADMIN`: lets the user opt into Device Admin, providing uninstall friction during testing.
* `SYSTEM_ALERT_WINDOW`: permits click-catching and warning overlays where the platform allows them.
* `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: keep the filtering service visible and alive on Android 14+.
* `POST_NOTIFICATIONS`: protection status and battery-survival reminders.
* `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: optional OEM battery-exemption reminder/settings deep link.
* `QUERY_ALL_PACKAGES`: lets a sideloaded personal tool present installed apps for a watched-app list. A Play Store build should replace this with package visibility declarations for selected packages.
* MediaProjection is a user-consent API rather than a normal manifest permission; the future capture worker must request its one-time system consent before sampling frames.

## Safety and limitations

Android deliberately prevents third-party apps from silently granting Accessibility, VPN, Device Admin, overlay, or MediaProjection access. The onboarding screen explains each capability and the user must approve its system prompt. OEM task killers may stop background components; keep the notification visible and use the battery settings reminder. The app is not a substitute for network, parental, or clinical safety controls.
