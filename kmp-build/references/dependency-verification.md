# Dependency Verification

## Quick Reference

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:dependencies` | Full dependency tree for all configurations |
| `./gradlew :composeApp:dependencies --configuration commonMainImplementation` | Dependencies for commonMain only |
| `./gradlew :composeApp:dependencies --configuration androidDebugRuntimeClasspath` | Android debug runtime dependencies |
| `./gradlew :composeApp:dependencies --configuration iosSimulatorArm64MainImplementation` | iOS dependencies |
| `./gradlew :composeApp:dependencyInsight --dependency <name>` | Trace why a specific dependency is included |

## Full Dependency Tree

Prints the resolved dependency tree for all configurations. The output is large -- pipe to a file for easier searching.

```bash
./gradlew :composeApp:dependencies > dependencies.txt
```

## Scoped Dependency Trees

### Common (shared) dependencies

```bash
./gradlew :composeApp:dependencies --configuration commonMainImplementation
```

### Android dependencies

```bash
# All Android configurations
./gradlew :composeApp:dependencies --configuration androidDebugRuntimeClasspath
./gradlew :composeApp:dependencies --configuration androidReleaseRuntimeClasspath
```

### iOS dependencies

```bash
./gradlew :composeApp:dependencies --configuration iosSimulatorArm64MainImplementation
./gradlew :composeApp:dependencies --configuration iosArm64MainImplementation
```

## Dependency Insight

Find out why a specific dependency is included and which version was selected.

```bash
# Why is a specific library included?
./gradlew :composeApp:dependencyInsight --dependency ktor-client-core

# Which version of kotlinx-coroutines was resolved?
./gradlew :composeApp:dependencyInsight --dependency kotlinx-coroutines-core
```

The output shows the dependency path from your project to the resolved artifact, including version conflict resolution.

## Checking for Version Conflicts

### Forced version upgrades

Look for lines with `->` in the dependency tree, which indicate version conflict resolution:

```bash
./gradlew :composeApp:dependencies --configuration commonMainImplementation 2>&1 | grep -- "->"
```

Example output:

```
+--- org.jetbrains.kotlinx:kotlinx-coroutines-core:X.Y.A -> X.Y.B
```

This means the older version was requested but Gradle resolved to the newer version due to a transitive dependency requiring it.

### Duplicate artifacts with different group IDs

Some libraries have been moved between group IDs (e.g., `androidx.compose.material3` vs `org.jetbrains.compose.material3`). Check for both:

```bash
./gradlew :composeApp:dependencies --configuration commonMainImplementation 2>&1 | grep "material3"
```

## Version Catalog Verification

The version catalog lives at `gradle/libs.versions.toml`. Verify it is consistent:

### Check for unused entries

```bash
# List all version catalog references in build files
grep -r "libs\." --include="*.kts" . | grep -oP 'libs\.\K[a-zA-Z0-9.]+' | sort -u > used-refs.txt
```

Compare this against the entries in `libs.versions.toml` to find orphaned entries.

### Check for duplicate library declarations

Multiple aliases pointing to the same Maven coordinate waste catalog space and cause confusion. Search for duplicate `module` values in the `[libraries]` section.

### Verify version alignment

For libraries that must share a version (e.g., all Ktor modules, all Koin modules), verify they use the same version reference:

```toml
# Right -- all Ktor modules share one version
[versions]
ktor = "..."

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }

# Wrong -- hardcoded versions that can drift apart
ktor-client-core = { module = "io.ktor:ktor-client-core", version = "X.Y.Z" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version = "A.B.C" }
```

## Refreshing Dependencies

Force Gradle to re-download dependencies (useful when a snapshot or changed artifact is cached):

```bash
./gradlew :composeApp:dependencies --refresh-dependencies
```

Warning: This is slow because it re-downloads everything. Use only when you suspect a cached artifact is stale.
