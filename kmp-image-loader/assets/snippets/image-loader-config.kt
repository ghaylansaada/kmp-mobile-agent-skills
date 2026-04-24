package {your.package}.di.modules

// Alternative ImageLoader configuration patterns.
// For the default authenticated configuration, see image-loader-module.md.

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

// --- Pattern 1: Public images only (no authentication) ---

@OptIn(ExperimentalCoilApi::class)
fun publicImageLoaderModule() = module {
    single<ImageLoader> {
        ImageLoader.Builder(context = get())
            .components {
                add(factory = KtorNetworkFetcherFactory(httpClient = get<HttpClient>()))
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

// --- Pattern 2: Custom cache sizes ---

@OptIn(ExperimentalCoilApi::class)
fun customCacheImageLoaderModule(
    memoryCachePercent: Double = 0.25,
    diskCacheMb: Long = 50L,
    diskCacheDirName: String = "coil_cache",
) = module {
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
                    .maxSizePercent(context = get(), percent = memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / diskCacheDirName)
                    .maxSizeBytes(diskCacheMb * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

// --- Pattern 3: Clearing caches (e.g., on logout) ---

// val imageLoader = koinInject<ImageLoader>()
// imageLoader.memoryCache?.clear()
// imageLoader.diskCache?.clear()
