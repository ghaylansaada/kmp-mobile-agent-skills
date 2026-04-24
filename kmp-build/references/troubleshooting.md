# Build Troubleshooting

## Common Build Failure Patterns

### 1. KSP Version Mismatch

**Error:** `The Kotlin version used for building KSP (X) is incompatible with the Kotlin version of your project (Y)`

**Fix:** Update the KSP version in `gradle/libs.versions.toml` to match the Kotlin version. The KSP version format is `<kotlin-version>-<ksp-release>`:

```toml
[versions]
kotlin = "..."          # e.g., "X.Y.Z"
ksp = "..."             # Must start with the Kotlin version, e.g., "X.Y.Z-1.0.N"
```

After updating, run a clean build:

```bash
./gradlew clean :composeApp:assembleDebug
```

### 2. Unresolved References After Dependency Change

**Error:** `Unresolved reference: SomeClass`

**Fix:** KSP-generated code (Room DAOs, Ktorfit services) requires regeneration:

```bash
./gradlew clean
./gradlew :composeApp:kspDebugKotlinAndroid  # regenerate Android KSP output
./gradlew :composeApp:assembleDebug
```

### 3. Kotlin Metadata Compilation Failure

**Error:** `e: ...Compilation failed. See the compiler error output for details` during `compileKotlinMetadata`

**Fix:** Metadata compilation compiles `commonMain` in isolation. Common causes:
- Platform-specific code in `commonMain` without `expect`/`actual`
- Missing dependency in the `commonMain` source set
- Type mismatch between `expect` declaration and `actual` implementation

Check that all `commonMain` imports resolve without platform-specific dependencies.

### 4. Compose Compiler Errors

**Error:** `Compose compiler plugin version is incompatible`

**Fix:** With Kotlin 2.0+, ensure the Compose compiler plugin is applied:

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

// build.gradle.kts (module)
plugins {
    alias(libs.plugins.kotlin.compose.compiler)
}
```

Do NOT set an explicit `composeCompiler` version -- it is bundled with the Kotlin plugin.

## Gradle Daemon Issues

### Daemon OOM

**Symptom:** Build fails intermittently with `OutOfMemoryError` or the Gradle daemon process is killed.

**Fix:** Increase memory in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -XX:MaxMetaspaceSize=512m
kotlin.daemon.jvmargs=-Xmx4g
```

Rule: Kotlin daemon memory must not exceed Gradle daemon memory.

### Stale Daemon

**Symptom:** Build behaves differently from a clean terminal, or uses old plugin versions.

**Fix:**

```bash
./gradlew --stop
./gradlew :composeApp:assembleDebug
```

### Multiple Daemon Instances

**Symptom:** Machine is slow during builds, multiple `GradleDaemon` processes visible.

**Fix:**

```bash
./gradlew --stop
# Verify all daemons are gone
jps | grep GradleDaemon
```

If daemons persist, kill them manually:

```bash
pkill -f GradleDaemon
```

## Cache Invalidation

### When to invalidate caches

- After upgrading Kotlin, Gradle, or KSP versions
- When builds produce stale or incorrect output
- When IDE and command-line builds disagree

### Incremental steps (try in order)

```bash
# Step 1: Clean build outputs
./gradlew clean

# Step 2: Clean + rebuild
./gradlew clean :composeApp:assembleDebug

# Step 3: Delete Gradle caches (last resort)
rm -rf ~/.gradle/caches/
rm -rf .gradle/
./gradlew :composeApp:assembleDebug
```

Step 3 forces re-download of all dependencies. Use only when steps 1-2 do not resolve the issue.

### IDE cache (IntelliJ / Android Studio)

If the IDE shows errors that the CLI build does not:
1. File > Invalidate Caches > Invalidate and Restart
2. After restart, wait for indexing to complete before judging results

## KSP / Compiler Version Mismatches

### Symptom

```
e: Incompatible classes were found in dependencies.
```

### Diagnosis

```bash
./gradlew :composeApp:dependencies --configuration kspDebugKotlinAndroid
```

Check that all KSP processors are compatible with the current Kotlin version.

### Common mismatches

| Plugin | Version coupling |
|--------|-----------------|
| KSP | Must start with the exact Kotlin version |
| Room KSP | Tied to the Room version, which requires a compatible KSP |
| Ktorfit KSP | Requires `compilerPluginVersion` set explicitly |

## Build Performance

### Diagnosing slow builds

```bash
# Generate a build scan
./gradlew :composeApp:assembleDebug --scan

# Profile the build
./gradlew :composeApp:assembleDebug --profile
```

The build scan (uploaded to scans.gradle.com) shows task-level timing, configuration time, and dependency resolution time. The `--profile` flag generates an HTML report at `build/reports/profile/`.

### Common slow build causes

1. **Configuration cache disabled** -- KSP requires it off, so this is expected overhead
2. **No build cache** -- Ensure `org.gradle.caching=true` in `gradle.properties`
3. **Full recompilation** -- Check that incremental compilation is not disabled
4. **Large generated source sets** -- Room and Ktorfit KSP output can be large; verify no redundant processors are running
