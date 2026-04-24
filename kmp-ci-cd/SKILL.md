---
name: kmp-ci-cd
description: >
  Use this skill when setting up CI/CD for a KMP mobile project using GitHub
  Actions or GitLab CI/CD. Activate when the user asks to "add CI," "automate
  the release," "configure caching," "set up Fastlane," "bump the version,"
  "set up GitLab CI/CD," or "add a GitLab pipeline." Covers dual-platform
  builds (Android on Ubuntu/Docker, iOS on macOS), Gradle and Kotlin/Native
  caching, Fastlane integration for Play Store and TestFlight, version bumping
  scripts, artifact management, and environment-based deployments. Does NOT
  cover Android signing setup (see kmp-release), iOS code signing details (see
  kmp-release), or build configuration flavors (see kmp-build-config).
compatibility: >
  KMP with Compose Multiplatform. GitHub Actions or GitLab CI/CD. Optional
  Fastlane for deployment.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP CI/CD

## Scope

Covers CI/CD pipelines for KMP mobile projects on GitHub Actions and GitLab CI/CD: dual-platform CI (Android on Ubuntu/Docker, iOS on macOS), release workflows (tag-triggered), Gradle and Kotlin/Native caching, Fastlane integration (Play Store, TestFlight, Firebase App Distribution), version bumping, artifact management, merge request / pull request pipelines, and environment-based deployments. Does not cover signing setup, build flavors, or platform-specific release details.

## When to use

- Setting up GitHub Actions for KMP builds and tests
- Configuring GitLab CI/CD pipelines for dual-platform builds
- Integrating Fastlane for automated deployment
- Adding Gradle caching to speed up CI builds
- Automating version bumping in CI pipelines
- Deploying to Google Play or TestFlight from CI
- Running unit and UI tests in CI with proper emulator/simulator setup
- Setting up code signing in CI environments

## Depends on

- KMP project with Compose Multiplatform (see kmp-project-setup)
- Android signing configured (see kmp-release)
- iOS code signing configured (see kmp-release)

## Workflow

1. **Configure secrets and Gradle properties** -- read references/setup.md
2. **Choose your CI platform** -- GitHub Actions (steps 3-5) or GitLab CI/CD (step 6)
3. **Copy CI workflow template (GitHub Actions)** -- use assets/templates/ci.yml
4. **Copy Android release workflow (GitHub Actions)** -- use assets/templates/release-android.yml
   _Skip if not deploying Android._
5. **Copy iOS release workflow (GitHub Actions)** -- use assets/templates/release-ios.yml
   _Skip if not deploying iOS._
6. **Set up GitLab CI/CD pipeline** -- read references/gitlab-ci.md, use assets/snippets/gitlab-ci.yml
   _Covers all stages: lint, test, build, deploy for both platforms._
7. **Configure Fastlane** -- read references/fastlane-config.md
   _Skip if using manual deployment._
8. **Set up version bumping** -- read references/version-bumping.md

## Gotchas

### General (all CI platforms)

1. **Gradle configuration cache is incompatible with KSP and several KMP plugins.** Set org.gradle.configuration-cache=false in gradle.properties or builds fail with cryptic serialization errors.

2. **Kotlin/Native compilation is ~10x slower than JVM.** Cache ~/.konan/ or every iOS CI run recompiles from scratch, adding 10-15 minutes.

3. **Always use --no-daemon on CI.** The daemon wastes memory on ephemeral runners and can cause OOM kills (exit code 137).

4. **Gradle wrapper directory (gradle/wrapper/) must be committed.** Mismatched wrapper versions between developers and CI cause silent build divergence.

5. **Use target-specific tasks (`:composeApp:assembleDebug`) not a single build task.** This enables parallel jobs and avoids unnecessary Kotlin/Native compilation on Android-only runs.

6. **`xcrun altool --upload-app` is deprecated starting Xcode 14.** Use the App Store Connect API directly or Fastlane upload_to_testflight with an API key to avoid breakage in future Xcode versions.

7. **Deriving versionCode / build number from `git tag -l | wc -l` is fragile.** Deleted or re-created tags cause version code collisions that Play Store and TestFlight reject. Use a monotonically increasing counter in gradle.properties or a CI run number instead.

8. **Firebase CLI `--token` flag is deprecated.** Migrate to Google Application Default Credentials or Workload Identity Federation for CI environments.

### GitHub Actions specific

9. **macOS runners cost 10x more than Linux.** Never run Android-only tasks on macOS. Split into separate jobs.

10. **Tag-triggered workflows must exist on the default branch.** Feature-branch workflows will not trigger on tag push.

11. **GitHub cache evicts after 7 days unused, 10 GB total limit per repository.**

### GitLab CI/CD specific

12. **GitLab shared runners are Linux-only.** iOS builds require a self-hosted macOS runner registered with the macos tag. Without one, iOS jobs stay in pending state indefinitely.

13. **GitLab CI/CD variables are the equivalent of GitHub secrets.** Set them in Settings > CI/CD > Variables with the Masked and Protected flags enabled.

14. **GitLab artifact expiry defaults to 30 days.** Set expire_in explicitly or artifacts disappear without warning. Use 7 days for test reports and 90 days for release binaries and symbolication files.

15. **The `cache:key` must be deterministic.** Using CI_COMMIT_REF_SLUG alone means cache is never shared across branches. Include a hash of Gradle files for dependency cache reuse.

16. **GitLab merge request pipelines require `rules:` with `if: $CI_PIPELINE_SOURCE == "merge_request_event"`.** This avoids duplicate pipelines (one for branch push, one for MR).

17. **GitLab Docker executor does not persist ~/.konan/ between jobs by default.** List the Konan path explicitly under cache:paths or Kotlin/Native recompiles every run.

18. **Self-hosted macOS runners must have Xcode, JDK, and the Gradle wrapper pre-installed.** Unlike GitHub-hosted runners, nothing is pre-provisioned.

19. **GitLab Releases API requires a `release-cli` image or the `release:` keyword.** Tag pipelines trigger automatically but detached pipelines on tags do not inherit branch variables.

## Assets

| Path | Load when... |
|---|---|
| references/setup.md | Configuring CI secrets and Gradle properties for CI environments |
| references/gitlab-ci.md | Setting up GitLab CI/CD pipeline for dual-platform builds |
| references/fastlane-config.md | Integrating Fastlane for Play Store or TestFlight deployment |
| references/version-bumping.md | Automating version bumping in CI pipelines |
| assets/templates/ci.yml | Scaffolding GitHub Actions CI workflow for KMP |
| assets/templates/release-android.yml | Scaffolding GitHub Actions Android release workflow |
| assets/templates/release-ios.yml | Scaffolding GitHub Actions iOS release workflow |
| assets/snippets/gitlab-ci.yml | Adding GitLab CI/CD pipeline configuration |

## Validation

### A. CI/CD Correctness (GitHub Actions)
- [ ] CI workflow runs Android job on ubuntu-latest and iOS job on macos-15
- [ ] Gradle cache key includes OS, all *.gradle* files, gradle-wrapper.properties, and gradle/libs.versions.toml
- [ ] Kotlin/Native ~/.konan/ cached separately for iOS jobs
- [ ] --no-daemon flag present on every Gradle invocation in workflow templates
- [ ] org.gradle.configuration-cache=false set in gradle.properties
- [ ] iOS build steps use CODE_SIGNING_ALLOWED=NO for CI debug builds
- [ ] iOS build sets ENABLE_BITCODE=NO (deprecated since Xcode 14)
- [ ] Release workflows trigger on v* tags and workflow_dispatch
- [ ] Test results uploaded with if: always() so failures are still reported
- [ ] Build matrix covers both Android and iOS targets
- [ ] Concurrency group configured with cancel-in-progress: true
- [ ] Artifact signing uses secrets decoded at build time, not committed files

### B. CI/CD Correctness (GitLab CI/CD)
- [ ] .gitlab-ci.yml placed at repository root
- [ ] Android jobs use a Docker image with Android SDK and JDK pre-installed
- [ ] iOS jobs tagged with macos and run on a self-hosted macOS runner
- [ ] Pipeline stages ordered correctly: lint, test, build, deploy
- [ ] Gradle and Konan caches configured with cache:key including file hash
- [ ] Merge request pipelines use rules: to avoid duplicate branch+MR pipelines
- [ ] CI/CD variables (KEYSTORE_BASE64, etc.) set as masked and protected
- [ ] Artifact paths and expire_in set for test reports, APK/AAB/IPA, and symbolication files
- [ ] Deploy jobs restricted to protected tags or the default branch
- [ ] --no-daemon flag present on every Gradle invocation

### C. Security
- [ ] No secrets, tokens, or credentials hardcoded in workflow or pipeline files
- [ ] Keystore and certificates decoded from base64 secrets at build time, not committed to repo
- [ ] Play Store service account key decoded from secret, not stored in repo
- [ ] iOS signing keychain created and deleted in the same job (GitHub: if: always(), GitLab: after_script)
- [ ] GitHub: GITHUB_TOKEN permissions scoped to minimum required
- [ ] GitLab: CI/CD variables use Masked and Protected flags
- [ ] Firebase tokens passed via environment variables from secrets / CI variables

### D. Performance
- [ ] Second CI run shows cache hits for Gradle and Konan caches
- [ ] Android and iOS jobs run in parallel (not sequential)
- [ ] iOS CI timeout accounts for Kotlin/Native compilation (45+ min)
- [ ] Gradle JVM args set appropriately (-Xmx4g -XX:+UseParallelGC)
- [ ] GitHub: concurrency group prevents duplicate runs on rapid pushes
- [ ] GitLab: interruptible: true set on CI jobs so auto-cancel works with the interruptible pipelines setting

### E. Integration
- [ ] Cross-references to kmp-project-setup, kmp-release, kmp-build-config are valid
- [ ] Fastlane lanes match the Gradle tasks used in workflow / pipeline templates
- [ ] Version bumping script updates both gradle.properties and iOS project version
- [ ] Tag push after version bump triggers release workflows / pipelines
- [ ] Artifact retention periods set (7 days test, 30 days release, 90 days symbolication)
