# iOS Privacy Manifests

## Overview

Apple requires a privacy manifest (`PrivacyInfo.xcprivacy`) for every app and SDK distributed through the App Store. This requirement became mandatory for App Store submissions in May 2024. Apps without a complete privacy manifest are rejected during App Store review.

## PrivacyInfo.xcprivacy Structure

The privacy manifest is a property list file with four top-level keys:

- **NSPrivacyTracking** -- Boolean indicating whether the app or SDK uses data for tracking as defined by the App Tracking Transparency framework. Set to `true` only if data is used to track users across apps and websites owned by other companies.
- **NSPrivacyTrackingDomains** -- Array of internet domains the app or SDK connects to that engage in tracking. If `NSPrivacyTracking` is `false`, this array must be empty.
- **NSPrivacyCollectedDataTypes** -- Array of dictionaries describing data types the app or SDK collects. Each entry specifies the data type, whether it is linked to the user's identity, whether it is used for tracking, and the purposes of collection. These values correspond to the App Store privacy nutrition labels.
- **NSPrivacyAccessedAPITypes** -- Array of dictionaries declaring which required-reason APIs the app or SDK uses and the approved reasons for each.

## Required-Reason APIs

Apple designates certain APIs as "required-reason" APIs. Any use of these APIs requires a declared reason in the privacy manifest. The categories relevant to KMP apps are:

**File timestamp APIs** -- `NSFileCreationDate`, `NSFileModificationDate`, `NSURLContentModificationDateKey`, `NSURLCreationDateKey`. Common reason: `C617.1` (access file timestamps inside the app container) or `DDA9.1` (display to the user).

**System boot time APIs** -- `systemUptime`, `mach_absolute_time`, `ProcessInfo.systemUptime`. Common reason: `35F9.1` (measure elapsed time within the app).

**Disk space APIs** -- `NSFileSystemFreeSize`, `NSFileSystemSize`, `NSURLVolumeAvailableCapacityKey`, `NSURLVolumeAvailableCapacityForImportantUsageKey`. Common reason: `E174.1` (check whether there is sufficient disk space before writing) or `85F4.1` (display to the user).

**User defaults APIs** -- `UserDefaults`. Common reason: `CA92.1` (access user defaults to read and write settings within the app) or `1C8F.1` (access managed app configuration via `UserDefaults`).

Each declared reason must match an approved reason code from Apple's documentation. Using an unapproved reason or omitting a reason causes App Store rejection.

## Fingerprinting Prohibition

Apple explicitly prohibits using device signals (including required-reason APIs) to create a unique device fingerprint. Even if each individual API access is declared with an approved reason, combining their outputs to derive a fingerprint violates the App Store Review Guidelines and results in rejection.

## KMP Framework Embedding

For a KMP project producing an XCFramework, the `PrivacyInfo.xcprivacy` file must be placed inside the framework bundle:

- Static framework: place `PrivacyInfo.xcprivacy` inside the `.framework` directory. Xcode merges it into the app's combined privacy manifest at build time.
- Dynamic framework: place `PrivacyInfo.xcprivacy` inside the `.framework` directory. The framework's manifest is included alongside the app's manifest.

Each SDK or framework must have its OWN privacy manifest. The app's privacy manifest covers the app's own API usage, but does not cover SDKs. Third-party SDKs that lack a privacy manifest cause a submission warning and may eventually cause rejection.

In a KMP project, the Kotlin/Native framework (`ComposeApp.framework` or equivalent) is a first-party framework. If any Kotlin code uses required-reason APIs (directly or via Kotlin Multiplatform libraries), the framework needs a privacy manifest declaring those usages.

## Placement in Xcode Project

For the main app target, place `PrivacyInfo.xcprivacy` in the app's resource bundle:

```
iosApp/iosApp/PrivacyInfo.xcprivacy
```

For the KMP framework, place it inside the framework output directory. This can be automated via a Gradle task that copies the file into the framework bundle during `embedAndSignAppleFrameworkForXcode`.

## Common KMP Privacy Manifest Entries

KMP apps commonly need to declare these API usages:

- **UserDefaults**: DataStore (Preferences) on iOS uses `NSUserDefaults` under the hood. Reason: `CA92.1`.
- **File timestamps**: Room database files, DataStore files, and any file I/O that reads metadata. Reason: `C617.1`.
- **Disk space**: Pre-flight checks before database writes or large file downloads. Reason: `E174.1`.
- **System boot time**: Ktor or other networking libraries that measure request duration using `mach_absolute_time`. Reason: `35F9.1`.

## Privacy Nutrition Labels

The `NSPrivacyCollectedDataTypes` entries in the privacy manifest correspond to the privacy nutrition labels displayed on the App Store. These labels describe what data the app collects, whether it is linked to the user, and the purposes of collection.

Categories include: contact info, health and fitness, financial info, location, sensitive info, contacts, user content, browsing history, search history, identifiers, usage data, diagnostics, and other data.

Configure these in the privacy manifest AND in App Store Connect. The values must be consistent -- discrepancies between the manifest and the App Store Connect declarations trigger review flags.

## Cross-Skill References

- **kmp-permissions** -- Permission usage descriptions in Info.plist are separate from the privacy manifest but both are reviewed during App Store submission.
- **kmp-datastore** -- DataStore and Room database access may require `UserDefaults` and file timestamp declarations in the privacy manifest.
- **kmp-networking** -- Ktor Darwin engine may use system boot time APIs for timing; tracking domains must be declared if applicable.
