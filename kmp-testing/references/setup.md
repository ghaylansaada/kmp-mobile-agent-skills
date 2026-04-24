# Setup Reference -- KMP Testing

> Always use the latest stable versions. Check the official release pages for current versions.

## Table of Contents

1. [Version Catalog](#version-catalog)
2. [Build Script](#build-script)
3. [Source Set Layout](#source-set-layout)
4. [Robolectric Configuration](#robolectric-configuration)
5. [iOS Test Source Set](#ios-test-source-set)
6. [Xcode Test Targets](#xcode-test-targets)
7. [Flow Wrapper for Swift](#flow-wrapper-for-swift)
8. [IDE Setup](#ide-setup)
9. [Checklist](#checklist)

---

## Version Catalog

Add all test dependencies to `gradle/libs.versions.toml`:

```toml
[versions]
turbine = "..."

[libraries]
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
koin-test-junit4 = { module = "io.insert-koin:koin-test-junit4", version.ref = "koin" }
```

The `kotlin-test` dependency is provided by the Kotlin plugin and does not need a manual
entry. The `kotlinx-coroutines-test` version MUST match `kotlinx-coroutines-core` --
use the same `version.ref` to prevent `NoClassDefFoundError`.

Use `mockk-android` (not plain `mockk`) for tests that mock Android framework types
like `Context`. The plain artifact lacks Android-aware class loader hooks.

---

## Build Script

In `composeApp/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }

        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.mockk.android)
            implementation(libs.koin.test.junit4)

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true  // required for Robolectric
            isReturnDefaultValues = true
        }
    }
}
```

`isIncludeAndroidResources = true` is mandatory for Robolectric. Without it,
`ApplicationProvider.getApplicationContext()` throws `Resources$NotFoundException`.

Screenshot testing (optional):

```kotlin
plugins {
    id("io.github.takahirom.roborazzi")
}
```

---

## Source Set Layout

```
composeApp/src/
  commonTest/kotlin/{your/package}/
    fakes/
      FakeAccountRepository.kt
      FakeNetworkClient.kt
    builders/
      TestDataBuilders.kt
    helpers/
      ParameterizedTestHelper.kt
    domain/
      SessionManagerTest.kt
    data/
      AccountRepositoryTest.kt
    presentation/
      AccountViewModelTest.kt
    network/
      ApiCallTest.kt
      ApiResultTest.kt
    integration/
      MockEngineFactory.kt
      IntegrationTestModule.kt
      AccountIntegrationTest.kt
      FullStackAccountTest.kt

  androidUnitTest/
    kotlin/{your/package}/
      ui/
        AccountScreenTest.kt
      robolectric/
        SessionManagerAndroidTest.kt
      mock/
        AccountViewModelMockTest.kt
      room/
        AccountDaoTest.kt
      screenshot/
        AccountScreenScreenshotTest.kt
      integration/
        AndroidIntegrationTest.kt
        RoomIntegrationTest.kt
    resources/
      robolectric.properties

  iosTest/kotlin/{your/package}/
    platform/
      IosSessionStorageTest.kt
      IosExternalStorageTest.kt

  androidDebug/
    AndroidManifest.xml
```

Fakes and builders in `commonTest` are automatically visible to `androidUnitTest`
and `iosTest` via the source set hierarchy. No extra configuration needed.

Shared integration test utilities (MockEngineFactory, IntegrationTestModule)
must live in `commonTest` to be visible to all platform test source sets.

---

## Robolectric Configuration

Create `composeApp/src/androidUnitTest/resources/robolectric.properties`:

```properties
sdk=35
application=org.robolectric.shadows.ShadowApplication
```

Pin the SDK to a single version. Testing multiple SDKs multiplies initialization
time (~10s per SDK on first run due to JAR downloads).

Create or verify `composeApp/src/androidDebug/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:name="android.app.Application" />
</manifest>
```

This manifest provides the minimal Application class needed by
`createComposeRule()` under Robolectric.

---

## iOS Test Source Set

The `iosTest` source set inherits from `commonTest` automatically:

```kotlin
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        iosTest {
            dependsOn(commonTest.get())
        }
    }
}
```

---

## Xcode Test Targets

**Unit Test Target (iosAppTests):**
1. File > New > Target > iOS > Unit Testing Bundle
2. Name: `iosAppTests`, Language: Swift

**UI Test Target (iosAppUITests):**
1. File > New > Target > iOS > UI Testing Bundle
2. Name: `iosAppUITests`, Language: Swift

Import the shared framework in Swift test files:

```swift
import XCTest
@testable import ComposeApp
```

The framework must be built before Swift tests can compile:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

In Xcode Build Settings > Framework Search Paths add:

```
$(SRCROOT)/../composeApp/build/bin/iosSimulatorArm64/debugFramework
```

---

## Flow Wrapper for Swift

If not using SKIE, create a wrapper so Swift can consume Kotlin Flows:

File: `composeApp/src/iosMain/kotlin/{your/package}/util/FlowWrapper.kt`

```kotlin
package {your.package}.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class FlowWrapper<T>(private val flow: Flow<T>) {
    private var job: Job? = null

    fun collect(
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        job = scope.launch {
            try {
                flow.collect { value -> onEach(value) }
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
```

Always call `wrapper.cancel()` in Swift test teardown to prevent coroutine leaks.

Kotlin suspend functions are bridged to Swift `async throws` in Kotlin 2.0+.
The `@Throws` annotation is required:

```kotlin
@Throws(Exception::class)
suspend fun fetchAccounts(): List<Account> { ... }
```

---

## IDE Setup

IntelliJ IDEA and Android Studio automatically detect test source sets. If tests
do not appear in the gutter:

1. File > Invalidate Caches > Invalidate and Restart
2. Ensure the Kotlin Multiplatform plugin is installed
3. Re-sync Gradle (elephant icon or `./gradlew --refresh-dependencies`)

---

## Checklist

- [ ] `kotlinx-coroutines-test`, `kotlin-test`, and `turbine` in `commonTest.dependencies`
- [ ] `ktor-client-mock` and `koin-test` in `commonTest.dependencies`
- [ ] `kotlinx-coroutines-test` shares `version.ref` with `kotlinx-coroutines-core`
- [ ] `robolectric.properties` exists at `composeApp/src/androidUnitTest/resources/`
- [ ] `isIncludeAndroidResources = true` set in `android.testOptions`
- [ ] `mockk-android` (not plain `mockk`) in androidUnitTest dependencies
- [ ] Debug AndroidManifest.xml exists with minimal Application declaration
- [ ] `iosTest` source set declared and depends on `commonTest`
- [ ] `iosAppTests` and `iosAppUITests` Xcode targets exist
- [ ] Shared framework builds for simulator
- [ ] Framework Search Paths set in Xcode
- [ ] FlowWrapper (or SKIE) configured for Swift Flow consumption
- [ ] IDE shows test gutter icons next to `@Test` annotations
