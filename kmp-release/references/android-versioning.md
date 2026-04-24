# Version Management and Build Commands

## Version properties in gradle.properties

```properties
# versionCode must STRICTLY INCREASE for Play Store uploads
app.versionCode=1
app.versionName=1.0
```

The build.gradle.kts signing config (see
[assets/snippets/signing-config.gradle.kts](../assets/snippets/signing-config.gradle.kts))
reads these properties via `project.findProperty()`.

## Bump script

Create `scripts/bump-version.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

GRADLE_PROPERTIES="gradle.properties"
BUMP_TYPE="${1:-patch}"

CURRENT_CODE=$(grep "app.versionCode" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
CURRENT_NAME=$(grep "app.versionName" "$GRADLE_PROPERTIES" | cut -d'=' -f2)

echo "Current version: $CURRENT_NAME ($CURRENT_CODE)"

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
PATCH="${PATCH:-0}"

case "$BUMP_TYPE" in
    major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
    patch) PATCH=$((PATCH + 1)) ;;
    *) echo "Usage: $0 [major|minor|patch]"; exit 1 ;;
esac

NEW_CODE=$((CURRENT_CODE + 1))
NEW_NAME="$MAJOR.$MINOR.$PATCH"

# Use portable sed: GNU sed (Linux) uses -i with no arg, BSD sed (macOS)
# requires -i ''. This pattern works on both.
if sed --version >/dev/null 2>&1; then
    # GNU sed
    sed -i "s/app.versionCode=.*/app.versionCode=$NEW_CODE/" "$GRADLE_PROPERTIES"
    sed -i "s/app.versionName=.*/app.versionName=$NEW_NAME/" "$GRADLE_PROPERTIES"
else
    # BSD sed (macOS)
    sed -i '' "s/app.versionCode=.*/app.versionCode=$NEW_CODE/" "$GRADLE_PROPERTIES"
    sed -i '' "s/app.versionName=.*/app.versionName=$NEW_NAME/" "$GRADLE_PROPERTIES"
fi

echo "New version: $NEW_NAME ($NEW_CODE)"
```

## Output locations

| Artifact | Path |
|----------|------|
| Debug APK | `composeApp/build/outputs/apk/debug/composeApp-debug.apk` |
| Release AAB | `composeApp/build/outputs/bundle/release/composeApp-release.aab` |
| Release APK | `composeApp/build/outputs/apk/release/composeApp-release.apk` |
| Mapping file | `composeApp/build/outputs/mapping/release/mapping.txt` |

## Mapping file for crash deobfuscation

Upload `mapping.txt` after every release build. Without it, production crash stack traces
are obfuscated and unreadable.

```bash
# Upload to Firebase Crashlytics
firebase crashlytics:mappingFile:upload \
  --app=<FIREBASE_APP_ID> \
  composeApp/build/outputs/mapping/release/mapping.txt
```

Or upload via Play Console: App bundle explorer > Downloads > Deobfuscation files.
