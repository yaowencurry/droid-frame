# Project Agent Notes

## Build Environment
- This project requires Java 17.
- On this machine, use the Homebrew JDK explicitly. Do not rely on `/usr/libexec/java_home`, because it may report that no Java Runtime is installed.
- Use this prefix for Gradle commands:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin:$PATH"
```

## Required Verification After Every Code Change
After any source code change, always rebuild both debug and release packages before reporting completion.

Run all of these from the repository root:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin:$PATH" \
./gradlew test :app:assembleDebug :app:assembleRelease
```

This is the preferred one-shot command because it verifies unit tests and produces both APK variants in one Gradle invocation.

## Build Outputs
- Debug APK:
  `app/build/outputs/apk/debug/app-debug.apk`
- Release APK:
  `app/build/outputs/apk/release/app-release.apk`
- Do not send or install `app-release-unsigned.apk`. Unsigned APKs are build intermediates and may show as an invalid install package.

## Release Signing
- The release build is intentionally signed with the local debug keystore so it can be installed directly for testing.
- This is not a Play Store or production distribution signature.
- If a production release is needed, configure a real release keystore and update this section before building.

## Version And Install Rules
- Every installable package change must increase `versionCode` in `app/build.gradle.kts`.
- Update `versionName` to describe the build when bumping `versionCode`.
- Android will reject installing an APK over an already installed app when the new `versionCode` is lower than the installed one.
- Android will reject updating an installed app if the signing certificate changes. Current debug and release builds use the debug keystore to avoid signature mismatch during local testing.
- If switching from an older package signed with a different key, uninstall the old app first or keep using the same signing key.
- Before handing an APK to the user, confirm that the release output is `app-release.apk`, not `app-release-unsigned.apk`.

## Notes For Future Agents
- Do not say the app is verified unless the command above has completed successfully in the current turn.
- If Gradle fails with "Unable to locate a Java Runtime", rerun with the explicit `JAVA_HOME` and `PATH` shown above.
- Do not ask the user to remind you to package. Rebuild debug and release after every implementation change.
