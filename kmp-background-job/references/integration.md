# Background Job Integration

## Koin DI Module

**File:** `di/modules/BackgroundModule.kt`

```kotlin
package {your.package}.di.modules

import {your.package}.core.background.BackgroundTaskScheduler
import {your.package}.core.background.SyncTask
import org.koin.dsl.module

fun backgroundModule() = module {
    single<BackgroundTaskScheduler> { BackgroundTaskScheduler(context = get()) }
    single<SyncTask> { SyncTask() }
}
```

Register in your `startKoin` block:

```kotlin
modules(
    backgroundModule(),
)
```

## iOSApp.swift

```swift
@main
struct iOSApp: App {
    init() {
        BackgroundTasksIosKt.registerAndScheduleBackgroundTasks()
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

## Android Application Class

If using a custom Application class, implement `Configuration.Provider`:

```kotlin
class MobileApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MobileApplication)
            modules(backgroundModule())
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
```

## Scheduling from a ViewModel

```kotlin
class SettingsViewModel(
    private val scheduler: BackgroundTaskScheduler,
) : ViewModel() {

    fun enableBackgroundSync() {
        scheduler.schedule(
            BackgroundTaskConfig(
                taskId = SyncTask.TASK_ID,
                intervalMinutes = 30,
                requiresNetwork = true,
                isPeriodic = true,
            ),
        )
    }

    fun disableBackgroundSync() {
        scheduler.cancel(SyncTask.TASK_ID)
    }

    fun triggerImmediateSync() {
        scheduler.schedule(
            BackgroundTaskConfig(
                taskId = SyncTask.TASK_ID,
                intervalMinutes = 0,
                requiresNetwork = true,
                isPeriodic = false,
            ),
        )
    }
}
```
