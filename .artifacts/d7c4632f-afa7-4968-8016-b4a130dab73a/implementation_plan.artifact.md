# Implementation Plan - Fix Gradle Sync Error and Update Gradle Version

The user is experiencing a Gradle sync error due to the use of the deprecated `jvmTarget` string assignment in `kotlinOptions` and a warning about the Gradle version being deprecated for future Kotlin releases.

## User Review Required

> [!IMPORTANT]
> The Kotlin version in the project is `2.4.20`, which is a very new/future version. This version enforces strict migration to the `compilerOptions` DSL. I will migrate the `kotlinOptions` block to the new `compilerOptions` syntax.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/HomePC/Documents/bex-market-native/app/build.gradle.kts)
- Replace the legacy `kotlinOptions` block with the new `compilerOptions` DSL inside the `kotlin` block (if applicable) or update the syntax to use `jvmTarget.set(...)`.
- Given the error message, I will migrate to the `compilerOptions` DSL as suggested.

#### [MODIFY] [gradle/wrapper/gradle-wrapper.properties](file:///C:/Users/HomePC/Documents/bex-market-native/gradle/wrapper/gradle-wrapper.properties)
- Update `distributionUrl` to use Gradle `8.14.4` to resolve the deprecation warning.

## Verification Plan

### Automated Tests
- Run `./gradlew build` to ensure the project compiles with the new DSL and Gradle version.
- Run `gradle_sync` to verify the sync error is resolved.

### Manual Verification
- Check the Gradle sync output in Android Studio to ensure no more warnings about the Gradle version or `jvmTarget`.
