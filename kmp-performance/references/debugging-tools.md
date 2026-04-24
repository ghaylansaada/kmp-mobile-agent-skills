# Debugging Tools for Performance

## StrictMode (Android)

StrictMode is a developer tool that detects accidental main-thread violations in Android. It catches performance problems at development time that would otherwise silently degrade the user experience or cause ANR (Application Not Responding) dialogs in production.

### What StrictMode Detects

**Thread policy violations (main thread):**

- Disk reads on the main thread
- Disk writes on the main thread
- Network calls on the main thread
- Slow or blocking calls that could cause ANR

**VM policy violations (process-wide):**

- Resource leaks (unclosed SQLiteCursor, closeable objects)
- Activity instance leaks
- Untagged sockets
- Cleartext network traffic

### Implementation

StrictMode is Android-only and must be enabled only in debug builds. Place the setup in the Android Application class or MainActivity, before any other initialization.

```kotlin
// androidMain -- Application.onCreate() or MainActivity.onCreate()
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build(),
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .detectActivityLeaks()
            .penaltyLog()
            .build(),
    )
}
```

### KMP Integration Pattern

Extract StrictMode setup into a dedicated function to keep the Application class clean. Enable it before Koin initialization so violations during DI setup are caught.

```kotlin
// In androidMain Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        enableStrictModeIfDebug()
        initKoin { /* ... */ }
    }
}

private fun enableStrictModeIfDebug() {
    if (!BuildConfig.DEBUG) return
    // StrictMode setup here
}
```

### Best Practices

- NEVER enable StrictMode in production -- it can crash the app or show dialogs to users
- Enable early in development to catch issues before they reach users
- Use `penaltyLog()` during development for non-intrusive detection; use `penaltyDeath()` for strict enforcement in CI builds
- Common KMP violations that StrictMode catches: Room migration on main thread, DataStore first read on main thread, Ktor synchronous configuration
- Fix pattern: move blocking work to `withContext(Dispatchers.IO)` or use suspend functions
- StrictMode violations appear in Logcat with the tag `StrictMode` -- filter by this tag during development

## Other Android Debugging Tools

### Android Studio Profiler

Built into Android Studio. Provides real-time profiling for:

- **CPU Profiler**: method tracing, flame charts, system trace recording
- **Memory Profiler**: heap dumps, allocation tracking, leak detection
- **Network Profiler**: request/response inspection, bandwidth usage, timing
- **Energy Profiler**: battery impact from wake locks, jobs, alarms, GPS

Use the profiler on release or benchmark builds for production-representative data. Debug builds include instrumentation overhead that distorts measurements.

### Layout Inspector

Inspects the Compose hierarchy at runtime:

- View recomposition counts per composable
- Inspect the composition tree structure
- Identify unnecessary recompositions from unstable parameters
- Verify that `@Stable`/`@Immutable` annotations are effective

In Android Studio: Tools -> Layout Inspector while the app is running.

### Perfetto

System-level tracing tool for analyzing performance across the entire Android stack:

- Thread scheduling and CPU core usage
- Binder transactions and IPC overhead
- Frame rendering pipeline (Choreographer, RenderThread)
- Custom trace sections added via `android.os.Trace`

Use Perfetto when Android Studio Profiler lacks the system-level detail needed to diagnose jank or startup delays. Access at ui.perfetto.dev or via Android Studio's system trace recording.

## iOS Equivalent

For iOS performance debugging, use Xcode Instruments. Coverage of Instruments (Time Profiler, Allocations, Leaks, System Trace) is in [memory-leaks.md](memory-leaks.md) and [setup.md](setup.md). There is no direct StrictMode equivalent on iOS -- use Instruments' Time Profiler to detect main-thread blocking calls.
