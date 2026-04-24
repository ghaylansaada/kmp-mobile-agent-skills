# Fastlane Configuration

> Fastlane configuration is CI-platform-agnostic. The Fastfile, Appfile, and Matchfile below work with both GitHub Actions and GitLab CI/CD. For GitHub Actions workflow integration see assets/templates/. For GitLab CI/CD pipeline integration see references/gitlab-ci.md.

## Fastfile

```ruby
# fastlane/Fastfile
default_platform(:android)

platform :android do
  lane :build_release do
    gradle(task: "bundle", build_type: "Release", project_dir: ".",
      properties: { "app.versionCode" => ENV["VERSION_CODE"] || "1",
                    "app.versionName" => ENV["VERSION_NAME"] || "1.0",
                    "flavor" => "prod" })
  end

  lane :deploy_internal do
    build_release
    upload_to_play_store(
      track: "internal",
      aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
      json_key: "fastlane/play-store-key.json",
      package_name: "{your.package}",
      skip_upload_metadata: true, skip_upload_images: true, skip_upload_screenshots: true,
      mapping: "composeApp/build/outputs/mapping/release/mapping.txt")
  end

  lane :distribute_firebase do
    gradle(task: "assemble", build_type: "Release", project_dir: ".",
      properties: { "app.versionCode" => ENV["VERSION_CODE"] || "1",
                    "app.versionName" => ENV["VERSION_NAME"] || "1.0", "flavor" => "prod" })
    firebase_app_distribution(
      app: ENV["FIREBASE_APP_ID"],
      apk_path: "composeApp/build/outputs/apk/release/composeApp-release.apk",
      groups: "stakeholders",
      service_credentials_file: ENV["GOOGLE_APPLICATION_CREDENTIALS"])
  end
end

platform :ios do
  before_all do
    setup_ci if ENV["CI"]
  end

  lane :sync_certificates do
    match(type: "appstore", readonly: is_ci)
  end

  lane :build do
    sync_certificates
    gradle(task: ":composeApp:linkReleaseFrameworkIosArm64", project_dir: "..",
      properties: { "flavor" => "prod" })
    build_app(project: "iosApp/iosApp.xcodeproj", scheme: "iosApp",
      configuration: "Release", export_method: "app-store",
      output_directory: "build/ipa", include_bitcode: false)
  end

  lane :beta do
    build
    api_key = app_store_connect_api_key(
      key_id: ENV["APP_STORE_CONNECT_KEY_ID"],
      issuer_id: ENV["APP_STORE_CONNECT_ISSUER_ID"],
      key_content: Base64.decode64(ENV["APP_STORE_CONNECT_KEY_BASE64"]))
    upload_to_testflight(api_key: api_key, ipa: "build/ipa/iosApp.ipa",
      skip_waiting_for_build_processing: true)
  end
end
```

## Appfile

```ruby
# fastlane/Appfile
json_key_file("fastlane/play-store-key.json")
package_name("{your.package}")
app_identifier("{your.package}")
```

## Matchfile

```ruby
# fastlane/Matchfile
git_url(ENV["MATCH_GIT_URL"] || "https://github.com/your-org/ios-certificates.git")
storage_mode("git")
type("appstore")
app_identifier("{your.package}")
```

## CI Integration

### GitHub Actions

```yaml
- name: Install Fastlane
  run: |
    gem install fastlane --no-document
    fastlane add_plugin firebase_app_distribution
  env:
    FASTLANE_SKIP_UPDATE_CHECK: "1"
    LC_ALL: en_US.UTF-8
```

### GitLab CI/CD

```yaml
before_script:
  - gem install fastlane --no-document
  - fastlane add_plugin firebase_app_distribution
variables:
  FASTLANE_SKIP_UPDATE_CHECK: "1"
  LC_ALL: "en_US.UTF-8"
```
