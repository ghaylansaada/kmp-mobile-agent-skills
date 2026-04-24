# Version Bumping

## Bump Script (scripts/bump-version.sh)

```bash
#!/usr/bin/env bash
set -euo pipefail

GRADLE_PROPERTIES="gradle.properties"
BUMP_TYPE="${1:-patch}"

CURRENT_CODE=$(grep "app.versionCode" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
CURRENT_NAME=$(grep "app.versionName" "$GRADLE_PROPERTIES" | cut -d'=' -f2)

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

# Portable sed -i: macOS requires '' after -i, Linux does not.
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/app.versionCode=.*/app.versionCode=$NEW_CODE/" "$GRADLE_PROPERTIES"
    sed -i '' "s/app.versionName=.*/app.versionName=$NEW_NAME/" "$GRADLE_PROPERTIES"
else
    sed -i "s/app.versionCode=.*/app.versionCode=$NEW_CODE/" "$GRADLE_PROPERTIES"
    sed -i "s/app.versionName=.*/app.versionName=$NEW_NAME/" "$GRADLE_PROPERTIES"
fi

if command -v agvtool &> /dev/null && [ -d "iosApp" ]; then
    cd iosApp
    agvtool new-version -all "$NEW_CODE"
    agvtool new-marketing-version "$NEW_NAME"
    cd ..
fi

echo "Bumped to $NEW_NAME ($NEW_CODE)"
echo "Next: git add gradle.properties iosApp/ && git commit && git tag -a v$NEW_NAME && git push --tags"
```

## Automated Version Bump Workflow (GitHub Actions)

```yaml
# .github/workflows/bump-version.yml
name: Bump Version
on:
  workflow_dispatch:
    inputs:
      bump_type:
        description: "Bump type"
        required: true
        type: choice
        options: [patch, minor, major]

jobs:
  bump:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { token: "${{ secrets.GITHUB_TOKEN }}" }
      - name: Bump version
        run: bash scripts/bump-version.sh ${{ github.event.inputs.bump_type }}
      - name: Read version
        id: v
        run: |
          echo "name=$(grep app.versionName gradle.properties | cut -d= -f2)" >> $GITHUB_OUTPUT
          echo "code=$(grep app.versionCode gradle.properties | cut -d= -f2)" >> $GITHUB_OUTPUT
      - name: Commit and tag
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add gradle.properties
          git commit -m "Bump version to ${{ steps.v.outputs.name }} (${{ steps.v.outputs.code }})"
          git tag -a "v${{ steps.v.outputs.name }}" -m "Release ${{ steps.v.outputs.name }}"
          git push origin main --tags
```

## Artifact Retention

| Artifact | Retention | Purpose |
|----------|-----------|---------|
| Test/lint results | 7 days | PR review |
| Release AAB/APK/IPA | 30 days | Backup |
| mapping.txt / dSYMs | 90 days | Crash symbolication |
