# Device Integrity Checker (Root / Jailbreak Detection)

Best-effort detection of rooted Android or jailbroken iOS devices.
Determined attackers can bypass these checks.

## commonMain -- DeviceIntegrityChecker.kt

```kotlin
package {your.package}.security

expect class DeviceIntegrityChecker {
    fun isDeviceCompromised(): Boolean
    fun getCompromiseReasons(): List<String>
}
```

## androidMain -- DeviceIntegrityChecker.android.kt

Uses `PackageManager.PackageInfoFlags.of()` for API 33+ compatibility.

```kotlin
package {your.package}.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

actual class DeviceIntegrityChecker(private val context: Context) {
    actual fun isDeviceCompromised(): Boolean = getCompromiseReasons().isNotEmpty()

    actual fun getCompromiseReasons(): List<String> = buildList {
        if (checkSuBinary()) add("su binary found")
        if (checkRootManagementApps()) add("root management app installed")
        if (checkTestKeys()) add("test-keys build detected")
        if (checkDangerousProps()) add("dangerous system properties set")
        if (checkBusybox()) add("busybox binary found")
    }

    private fun checkSuBinary(): Boolean = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su",
        "/data/local/su", "/su/bin/su",
    ).any { File(it).exists() }

    private fun checkRootManagementApps(): Boolean {
        val packages = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
        )
        val pm = context.packageManager
        return packages.any { pkg ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun checkTestKeys(): Boolean =
        Build.TAGS?.contains("test-keys") == true

    private fun checkDangerousProps(): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("getprop", "ro.debuggable"))
            .inputStream.bufferedReader().readText().trim() == "1"
    } catch (_: Exception) {
        false
    }

    private fun checkBusybox(): Boolean =
        listOf("/system/xbin/busybox", "/system/bin/busybox")
            .any { File(it).exists() }
}
```

## iosMain -- DeviceIntegrityChecker.ios.kt

Uses file-path and URL-scheme checks. Does NOT call `fork()` because modern iOS
(post-iOS 9) kills processes that call `fork()`, crashing the app on non-jailbroken
devices. The `canOpenURL` check for Cydia requires adding `cydia` to
`LSApplicationQueriesSchemes` in `Info.plist`.

```kotlin
package {your.package}.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual class DeviceIntegrityChecker {
    actual fun isDeviceCompromised(): Boolean = getCompromiseReasons().isNotEmpty()

    actual fun getCompromiseReasons(): List<String> = buildList {
        if (checkCydiaInstalled()) add("Cydia app detected")
        if (checkSuspiciousPaths()) add("Suspicious file paths found")
        if (checkWriteAccess()) add("Write access to system paths")
        if (checkDynamicLibraries()) add("Suspicious dynamic libraries loaded")
    }

    private fun checkCydiaInstalled(): Boolean {
        val url = NSURL.URLWithString("cydia://package/com.example.package")
            ?: return false
        return UIApplication.sharedApplication.canOpenURL(url)
    }

    private fun checkSuspiciousPaths(): Boolean {
        val paths = listOf(
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt",
            "/private/var/lib/apt/",
            "/usr/bin/ssh",
            "/private/var/stash",
        )
        val fm = NSFileManager.defaultManager
        return paths.any { fm.fileExistsAtPath(it) }
    }

    private fun checkWriteAccess(): Boolean = try {
        val testPath = "/private/jailbreak_test"
        val fm = NSFileManager.defaultManager
        val created = fm.createFileAtPath(
            testPath,
            contents = null,
            attributes = null,
        )
        if (created) fm.removeItemAtPath(testPath, error = null)
        created
    } catch (_: Exception) {
        false
    }

    private fun checkDynamicLibraries(): Boolean {
        val suspiciousLibs = listOf(
            "MobileSubstrate",
            "SubstrateLoader",
            "CydiaSubstrate",
            "libhooker",
        )
        val envDyld = platform.posix.getenv("DYLD_INSERT_LIBRARIES")
            ?.let { kotlinx.cinterop.toKString(it) }
        return envDyld != null ||
            suspiciousLibs.any { lib ->
                NSFileManager.defaultManager.fileExistsAtPath(
                    "/Library/MobileSubstrate/DynamicLibraries/$lib.dylib",
                )
            }
    }
}
```
