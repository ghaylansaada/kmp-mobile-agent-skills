#!/usr/bin/env bash
# =============================================================================
# XCFramework build script for ComposeApp
# Run from the project root: bash scripts/build-xcframework.sh [flavor]
#
# Produces: build/xcframework/ComposeApp.xcframework
# Includes: iosArm64 (device) + iosSimulatorArm64 (Apple Silicon simulator)
#
# GOTCHA: Do NOT include both iosSimulatorArm64 and iosX64. They share the
# same platform identifier and xcodebuild -create-xcframework will fail with:
# "Both 'ios-arm64-simulator' and 'ios-x86_64-simulator' represent the same
# library." Use iosSimulatorArm64 only.
#
# GOTCHA: Fat frameworks (lipo) that contain simulator slices will be REJECTED
# by App Store. XCFramework separates slices correctly and is the required
# format for distribution.
# =============================================================================
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="$PROJECT_ROOT/build/xcframework"
FLAVOR="${1:-prod}"

echo "=== Building ComposeApp XCFramework (flavor=$FLAVOR) ==="
echo ""

# Step 1: Build release frameworks
echo "[1/3] Building release framework for iosArm64 (device)..."
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" \
  :composeApp:linkReleaseFrameworkIosArm64 \
  -Pflavor="$FLAVOR" \
  --no-daemon

echo "[2/3] Building release framework for iosSimulatorArm64 (simulator)..."
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" \
  :composeApp:linkReleaseFrameworkIosSimulatorArm64 \
  -Pflavor="$FLAVOR" \
  --no-daemon

# Step 2: Create XCFramework
echo "[3/3] Creating XCFramework..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

xcodebuild -create-xcframework \
  -framework "$PROJECT_ROOT/composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework" \
  -framework "$PROJECT_ROOT/composeApp/build/bin/iosSimulatorArm64/releaseFramework/ComposeApp.framework" \
  -output "$OUTPUT_DIR/ComposeApp.xcframework"

echo ""
echo "=== Done ==="
echo "XCFramework: $OUTPUT_DIR/ComposeApp.xcframework"
echo ""
echo "Contents:"
ls -la "$OUTPUT_DIR/ComposeApp.xcframework/"
