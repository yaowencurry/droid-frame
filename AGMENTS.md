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
  `app/build/outputs/apk/release/app-release-unsigned.apk`

## Release Signing
- The current project has no release signing config.
- `:app:assembleRelease` produces `app-release-unsigned.apk`.
- If a signed installable/distributable release is needed, configure a release keystore first, then rebuild release.

## Notes For Future Agents
- Do not say the app is verified unless the command above has completed successfully in the current turn.
- If Gradle fails with "Unable to locate a Java Runtime", rerun with the explicit `JAVA_HOME` and `PATH` shown above.
- Do not ask the user to remind you to package. Rebuild debug and release after every implementation change.
