# DroidFrame

Native Android screenshot framing app implemented with Kotlin, Jetpack Compose, and an offline rendering pipeline.

## What is included

- Multi-image import from Android Photo Picker.
- `ACTION_SEND` and `ACTION_SEND_MULTIPLE` image share targets.
- Offline device matching from screenshot resolution and aspect ratio.
- Local frame manifest for Google Pixel, Samsung Galaxy, Xiaomi, OnePlus, vivo, and OPPO devices.
- Single or proportional multi-device canvas layout.
- Transparent PNG and JPEG export paths.
- MediaStore save plus Android Sharesheet sharing.
- Pure Kotlin `:core` module with unit tests for manifest parsing, matching, layout, and export configuration.

## Build

Install Android SDK and create `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

Then run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :core:test :app:assembleDebug
```

On machines without Android SDK, the core test suite can still run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :core:test
```

## Release APK

APK files are published through GitHub Releases, not committed to the repository.
To publish a downloadable APK, bump `versionCode` and `versionName`, commit the change, then push a version tag:

```bash
git tag v0.3.2
git push origin v0.3.2
```

GitHub Actions will run tests, build `app/build/outputs/apk/release/app-release.apk`, and attach it to the release as `DroidFrame-v0.3.2.apk`.

## Asset strategy

The first implementation renders high-quality programmatic device shells from the frame manifest. The manifest already includes the stable schema needed to replace programmatic shells with licensed PNG frame assets later: device dimensions, screenshot safe area, brand/name, and color asset IDs.
