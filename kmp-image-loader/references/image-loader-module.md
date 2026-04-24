# ImageLoader Module and Composable

## imageLoaderModule

**File:** `commonMain/kotlin/{your/package}/di/modules/ImageLoaderModule.kt`

```kotlin
package {your.package}.di.modules

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import {your.package}.core.image.ImageAuthInterceptor
import io.ktor.client.HttpClient
import okio.FileSystem
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(ExperimentalCoilApi::class)
fun imageLoaderModule() = module {
    single<ImageAuthInterceptor> {
        ImageAuthInterceptor(storageService = get())
    }

    single<ImageLoader> {
        ImageLoader.Builder(context = get())
            .components {
                add(interceptor = get<ImageAuthInterceptor>())
                add(factory = KtorNetworkFetcherFactory(
                    httpClient = get<HttpClient>(named(QUALIFIER_NAME_AUTHORIZATION_CLIENT)),
                ))
                add(factory = SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context = get(), percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil_cache")
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
```

| Component | Purpose |
|---|---|
| ImageAuthInterceptor | Resolves signed URLs, handles expiration retry |
| KtorNetworkFetcherFactory | HTTP image fetching via authenticated Ktor client |
| SvgDecoder.Factory | SVG image decoding alongside raster formats |
| MemoryCache (0.25) | 25% of available RAM for bitmap cache |
| DiskCache (50MB) | Persistent cache in OS temp directory |

## AppAsyncImage Composable

**File:** `commonMain/kotlin/{your/package}/ui/components/AppImage.kt`

```kotlin
package {your.package}.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

@Composable
fun AppAsyncImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(path)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = { state ->
            println("Image failed: ${state.result.throwable}")
        },
    )
}
```

`LocalPlatformContext.current` provides the platform context needed by Coil to resolve resources and create bitmap pools. The `path` can be an app-specific path (e.g., `/uploads/profile_123.jpg`) resolved by the interceptor, or a full URL loaded directly.
