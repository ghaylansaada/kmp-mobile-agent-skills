# Android Baseline Profiles

Baseline Profiles require BOTH `ProfileInstaller` library AND a Macrobenchmark test. Adding one without the other means no profile is ever applied. The test must run on a physical device or API 28+ emulator.

## BaselineProfileGenerator

```kotlin
package {your.package}.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(packageName = "{your.package}") {
            pressHome()
            startActivityAndWait()
            // Add critical user journeys here.
            // Each journey adds executed methods to the profile.
        }
    }
}
```

## Startup Benchmark

```kotlin
package {your.package}.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupWithBaselineProfile() {
        rule.measureRepeated(
            packageName = "{your.package}",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.COLD,
            iterations = 10,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    @Test
    fun startupWithoutCompilation() {
        rule.measureRepeated(
            packageName = "{your.package}",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 10,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
```

## Deployment

Generated profile goes to `composeApp/src/main/baseline-prof.txt`. It is automatically included in the APK and processed by ART during installation.

### Verification

After building a release APK, verify `baseline.prof` is included by inspecting the APK contents.
