# Memory Leak Detection

## Android: LeakCanary

LeakCanary is zero-config. Adding the dependency automatically installs leak detection. Optional customization (place in debug source set only):

```kotlin
package {your.package}

import android.app.Application
import leakcanary.LeakCanary

class DebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LeakCanary.config = LeakCanary.config.copy(
            retainedVisibleThreshold = 3,
            dumpHeapWhenDebugging = false
        )
    }
}
```

## Common Leak Patterns in KMP

**1. ViewModel leaks via coroutine scope:**
- Wrong: `launch(Dispatchers.IO) { ... }`
- Right: `viewModelScope.launch { ... }`

**2. Transfer task leaks:**
BaseTransferTask creates a SupervisorJob tied to parentScope. Cancel `taskJob` in `onCleared()` or when the screen exits.

**3. Compose state leaks:**
- Wrong: `remember { LargeDataSet(repository.getAll()) }`
- Right: Use `collectAsState` for reactive data

**4. Koin singleton leaks:**
Singletons live for the entire app lifecycle. Never store screen-scoped data in singletons.

**5. Kotlin/Native -- no escape analysis:**
Every object allocation goes to heap. Avoid allocations in tight loops in shared code targeting iOS.

## iOS: Xcode Instruments

No additional dependencies. Use Product -> Profile (Cmd+I) in Xcode:

- **Time Profiler**: CPU usage and hot code paths
- **Allocations**: Memory allocation tracking
- **Leaks**: Memory leak detection
- **System Trace**: Thread activity and scheduling

Always profile on Release builds. Debug builds include instrumentation that distorts measurements.

## Troubleshooting

**"LeakCanary notification in release build"** -- Change `implementation` to `debugImplementation`. Verify that the release runtime classpath does not include leakcanary.

**"Memory grows during transfer tasks":**
1. Verify chunk buffers are released after processing
2. Add maximum sample window to SpeedEstimator
3. Ensure Semaphore limits concurrent chunks in memory
4. Check Ktor response body is consumed and closed
