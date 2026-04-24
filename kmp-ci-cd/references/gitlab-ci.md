# GitLab CI/CD for KMP Projects

> This guide covers GitLab CI/CD pipelines for Kotlin Multiplatform mobile projects. For GitHub Actions, see the workflow templates in assets/templates/. For Fastlane integration applicable to both platforms, see references/fastlane-config.md.

## Pipeline File Location

GitLab expects `.gitlab-ci.yml` at the repository root. A complete template is provided at assets/snippets/gitlab-ci.yml.

## Pipeline Stages

```
stages:
  - lint
  - test
  - build
  - deploy
```

**lint** -- static analysis and code quality checks (Android lint, detekt).
**test** -- unit tests for both Android and iOS (shared KMP tests run on both).
**build** -- assembles release artifacts (APK, AAB, IPA).
**deploy** -- uploads to Play Store, TestFlight, Firebase App Distribution, or creates a GitLab Release.

## CI/CD Variables

Set these in Settings > CI/CD > Variables. Enable the **Masked** flag for all secrets and the **Protected** flag for variables that should only be available on protected branches/tags.

### Android

| Variable | Description | Flags |
|----------|-------------|-------|
| `KEYSTORE_BASE64` | Base64-encoded release.keystore | Masked, Protected |
| `KEYSTORE_PASSWORD` | Store password | Masked, Protected |
| `KEY_ALIAS` | Key alias | Protected |
| `KEY_PASSWORD` | Key password | Masked, Protected |
| `FIREBASE_APP_ID` | Firebase Android app ID | Protected |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Base64-encoded Firebase service account JSON | Masked, Protected |
| `PLAY_STORE_KEY_JSON` | Base64-encoded Play Store service account JSON | Masked, Protected |

### iOS

| Variable | Description | Flags |
|----------|-------------|-------|
| `IOS_CERTIFICATE_BASE64` | Base64-encoded .p12 distribution cert | Masked, Protected |
| `IOS_CERTIFICATE_PASSWORD` | .p12 password | Masked, Protected |
| `IOS_PROVISION_PROFILE_BASE64` | Base64-encoded .mobileprovision | Masked, Protected |
| `KEYCHAIN_PASSWORD` | Temporary keychain password | Masked, Protected |
| `APP_STORE_CONNECT_KEY_ID` | App Store Connect API key ID | Protected |
| `APP_STORE_CONNECT_ISSUER_ID` | App Store Connect issuer ID | Protected |
| `APP_STORE_CONNECT_KEY_BASE64` | Base64-encoded App Store Connect .p8 key | Masked, Protected |
| `FIREBASE_IOS_APP_ID` | Firebase iOS app ID | Protected |

### Encoding secrets

```bash
# Linux
base64 -w 0 release.keystore | xclip -selection clipboard

# macOS (base64 does not support -w flag)
base64 -i release.keystore | tr -d '\n' | pbcopy
```

## Runner Requirements

### Android jobs

Android jobs run on standard GitLab shared runners (Linux Docker executor). Use a Docker image that includes the Android SDK and JDK. Several community images exist; choose one that matches your project's compile and target SDK versions.

```yaml
android-build:
  image: "..."  # Docker image with Android SDK + JDK
  tags:
    - docker
```

### iOS jobs

iOS builds require macOS with Xcode. GitLab shared runners do not provide macOS. You must register a self-hosted macOS runner and tag it `macos`.

```yaml
ios-build:
  tags:
    - macos
```

The self-hosted runner must have pre-installed:
- Xcode (matching your project's minimum version)
- JDK (version matching your project's Gradle configuration)
- The Gradle wrapper (committed in gradle/wrapper/ and executable)

## Caching Strategy

GitLab caching uses the `cache:` directive with a `key:` for invalidation.

```yaml
.gradle-cache: &gradle-cache
  cache:
    - key:
        files:
          - gradle/wrapper/gradle-wrapper.properties
          - gradle/libs.versions.toml
          - "**/*.gradle.kts"
        prefix: gradle
      paths:
        - .gradle-home/caches/
        - .gradle-home/wrapper/
        - .kotlin/
      policy: pull-push

.konan-cache: &konan-cache
  cache:
    - key:
        files:
          - gradle/libs.versions.toml
        prefix: konan
      paths:
        - .konan/
      policy: pull-push
```

Key points:
- GitLab caches are per-runner by default. Enable distributed caching (S3, GCS) for shared caches across runners.
- Use `GRADLE_USER_HOME: "$CI_PROJECT_DIR/.gradle-home"` to place the Gradle home inside the project directory where GitLab can cache it.
- Konan cache (`~/.konan/` or `$CI_PROJECT_DIR/.konan/`) must be listed separately. Set `KONAN_DATA_DIR: "$CI_PROJECT_DIR/.konan"` so it lands within the cacheable project directory.
- GitLab has no total cache size limit per project (unlike GitHub's 10 GB), but individual cache archives are limited by runner configuration.

## Artifact Management

```yaml
artifacts:
  paths:
    - composeApp/build/outputs/apk/release/*.apk
    - composeApp/build/outputs/bundle/release/*.aab
  reports:
    junit: composeApp/build/test-results/**/TEST-*.xml
  expire_in: 30 days
```

| Artifact | expire_in | Purpose |
|----------|-----------|---------|
| Test/lint reports (JUnit XML) | 7 days | MR review, test tab |
| Release APK/AAB/IPA | 30 days | Download backup |
| mapping.txt / dSYMs | 90 days | Crash symbolication |

GitLab displays JUnit XML reports directly in the merge request "Tests" tab when configured under `reports:junit`.

## Merge Request Pipelines vs Branch Pipelines

Without explicit `rules:`, GitLab creates a pipeline for every push AND every merge request update, causing duplicate runs. Use rules to separate them:

```yaml
.mr-rules:
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH

.tag-rules:
  rules:
    - if: $CI_COMMIT_TAG =~ /^v.*/
```

- CI jobs (lint, test, build-debug) use `.mr-rules` -- they run on MR updates and pushes to the default branch.
- Deploy jobs use `.tag-rules` -- they run only when a version tag is pushed.

## Release Pipeline

Tag-triggered jobs handle release builds and deployment:

```yaml
deploy-android:
  stage: deploy
  rules:
    - if: $CI_COMMIT_TAG =~ /^v.*/
  script:
    - # Extract version from tag, decode keystore, build, deploy
  environment:
    name: production
    url: "https://play.google.com/store/apps/details?id={your.package}"
```

### GitLab Releases

Use the `release:` keyword to create a GitLab Release attached to the tag:

```yaml
create-release:
  stage: deploy
  image: registry.gitlab.com/gitlab-org/release-cli:latest
  rules:
    - if: $CI_COMMIT_TAG =~ /^v.*/
  script:
    - echo "Creating release for $CI_COMMIT_TAG"
  release:
    tag_name: $CI_COMMIT_TAG
    name: "Release $CI_COMMIT_TAG"
    description: "Release $CI_COMMIT_TAG"
    assets:
      links:
        - name: "Android APK"
          url: "${CI_PROJECT_URL}/-/jobs/${CI_JOB_ID}/artifacts/file/composeApp/build/outputs/apk/release/composeApp-release.apk"
```

## Environment-Specific Deployments

GitLab environments enable tracking deployments to staging and production:

```yaml
deploy-staging:
  stage: deploy
  environment:
    name: staging
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH

deploy-production:
  stage: deploy
  environment:
    name: production
  rules:
    - if: $CI_COMMIT_TAG =~ /^v.*/
  when: manual
```

Use `when: manual` for production deploys to require an explicit click in the GitLab UI.

## Conditional Execution with rules:

Restrict jobs to run only when relevant files change:

```yaml
android-build:
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
      changes:
        - composeApp/**/*
        - shared/**/*
        - gradle/**/*
        - "*.gradle.kts"
        - gradle.properties
```

This avoids running Android builds when only iOS-specific or documentation files changed.

## Fastlane Integration

Fastlane works identically on GitLab and GitHub. Install it in the `before_script`:

```yaml
.fastlane-setup:
  before_script:
    - gem install fastlane --no-document
    - fastlane add_plugin firebase_app_distribution
  variables:
    FASTLANE_SKIP_UPDATE_CHECK: "1"
    LC_ALL: "en_US.UTF-8"
```

See references/fastlane-config.md for Fastfile, Appfile, and Matchfile configuration. The same Fastlane lanes work regardless of whether the CI platform is GitHub Actions or GitLab CI/CD.

## KMP-Specific Considerations

### Kotlin/Native compilation cache

Kotlin/Native downloads and caches its compiler and platform libraries in the Konan directory. On GitLab, redirect this to the project directory so it is cacheable:

```yaml
variables:
  KONAN_DATA_DIR: "$CI_PROJECT_DIR/.konan"
```

Without this, every iOS CI run downloads ~500 MB of Kotlin/Native dependencies and recompiles platform libraries from scratch.

### Shared module incremental compilation

The shared KMP module compiles for multiple targets. On CI, incremental compilation can cause stale output issues. Consider adding `kotlin.incremental=false` in CI-specific Gradle properties, or ensure the cache key invalidates properly when source files change.

### Parallel target compilation

Android and iOS targets can compile in separate GitLab jobs running in parallel. This is the default when they are defined as independent jobs in the same stage. Do not combine them into a single job -- the Android job does not need macOS, and combining them wastes expensive macOS runner time.

## Self-Hosted macOS Runner Setup

1. Install GitLab Runner on the macOS machine.
2. Register it with your GitLab instance using a project or group runner token.
3. Use the `shell` executor (Docker is not available on macOS for iOS builds).
4. Add the tag `macos` during registration.
5. Ensure Xcode, JDK, and Ruby (for Fastlane) are installed.
6. The runner user must have Keychain access for code signing operations.
7. Consider using `launchd` to keep the runner running as a service.

```bash
# Registration example (use your own URL and token)
gitlab-runner register \
  --url "https://gitlab.com/" \
  --registration-token "..." \
  --executor "shell" \
  --tag-list "macos" \
  --description "macOS runner for iOS builds"
```
