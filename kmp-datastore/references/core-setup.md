# Core Setup: DataStore

## DataStoreFactory Interface (commonMain)

```kotlin
package {your.package}.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

interface DataStoreFactory {
    fun create(): DataStore<Preferences>
}
```

## Common createDataStore Helper (commonMain)

```kotlin
package {your.package}.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

fun createDataStore(
    producePath: () -> String,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() },
    )

internal const val dataStoreFileName = "session.preferences_pb"
```

## Android Implementation (androidMain)

```kotlin
package {your.package}.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath },
)
```

## iOS Implementation (iosMain)

```kotlin
package {your.package}.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory).path + "/$dataStoreFileName"
    },
)
```
