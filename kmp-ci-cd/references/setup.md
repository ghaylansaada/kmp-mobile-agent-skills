# CI/CD Setup: Secrets and Configuration

> **Note:** Version numbers are intentionally omitted. Always use the latest stable release from the official documentation.

> This page covers GitHub Actions setup. For GitLab CI/CD setup, see references/gitlab-ci.md.

## Repository Structure (GitHub Actions)

```
.github/workflows/
  ci.yml                  -- PR and push checks
  release-android.yml     -- Android release (tag-triggered)
  release-ios.yml         -- iOS release (tag-triggered)
```

For GitLab CI/CD, a single `.gitlab-ci.yml` at the repository root replaces all workflow files. See assets/snippets/gitlab-ci.yml for the template.

## Required GitHub Secrets

### Android

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded release.keystore |
| `KEYSTORE_PASSWORD` | Store password |
| `KEY_ALIAS` / `KEY_PASSWORD` | Key alias and password |
| `FIREBASE_APP_ID` | Firebase Android app ID |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Base64-encoded Firebase service account JSON (replaces deprecated CLI token) |
| `PLAY_STORE_KEY_JSON` | Base64-encoded service account JSON |

### iOS

| Secret | Description |
|--------|-------------|
| `IOS_CERTIFICATE_BASE64` | Base64-encoded .p12 distribution cert |
| `IOS_CERTIFICATE_PASSWORD` | .p12 password |
| `IOS_PROVISION_PROFILE_BASE64` | Base64-encoded .mobileprovision |
| `KEYCHAIN_PASSWORD` | Temporary keychain password |
| `APP_STORE_CONNECT_KEY_ID` / `ISSUER_ID` / `KEY_BASE64` | App Store Connect API |
| `FIREBASE_IOS_APP_ID` | Firebase iOS app ID |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Base64-encoded Firebase service account JSON (shared with Android if same project) |

### Encoding secrets

```bash
# Linux
base64 -w 0 release.keystore | xclip -selection clipboard
base64 -w 0 Certificates.p12 | xclip -selection clipboard

# macOS (base64 does not support -w flag)
base64 -i release.keystore | tr -d '\n' | pbcopy
base64 -i Certificates.p12 | tr -d '\n' | pbcopy
```

## Gradle Properties for CI

```properties
org.gradle.caching=true
org.gradle.configuration-cache=false
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
app.versionCode=1
app.versionName=1.0
```

## Caching Strategy

| Path | Contents | Impact |
|------|----------|--------|
| `~/.gradle/caches` + `wrapper` | Dependencies, wrapper | Saves 2-5 min |
| `.kotlin/` | Kotlin compiler caches | Saves 1-3 min |
| `~/.konan/` | Kotlin/Native compiler (iOS only) | Saves 10-15 min |

Cache key: OS + hash of Gradle files + version catalog. GitHub evicts after 7 days unused, 10 GB total limit. GitLab caching uses a different mechanism -- see references/gitlab-ci.md for the GitLab-specific caching strategy.

## Branch Protection (Recommended)

### GitHub

- Require status checks: `android-build`, `ios-build`
- Require branches up to date
- Require 1 PR review

### GitLab

- Set merge request approvals: 1 required
- Add `lint-android`, `test-android`, `test-ios` as required pipeline jobs in Settings > Merge Requests
- Enable "Pipelines must succeed" merge check

## Tag-Based Release Trigger

```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

GitHub: Workflow YAML must exist on the default branch for tag triggers to work. GitLab: Tag pipelines run automatically when `.gitlab-ci.yml` contains matching `rules:` on the default branch.
