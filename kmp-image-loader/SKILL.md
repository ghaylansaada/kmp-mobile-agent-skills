---
name: kmp-image-loader
description: >
  Image loading with Coil 3 for KMP -- ImageLoader singleton configuration, Ktor network
  fetching, SVG decoding, memory/disk caching, signed URL interceptor with retry, and
  AppAsyncImage composable. Activate when loading images from network or local storage,
  configuring image caching, displaying SVGs, handling authenticated image URLs, or creating
  reusable image composables.
compatibility: >
  KMP with Coil 3 and Compose Multiplatform. Requires Ktor for network image fetching.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Image Loader

## When to use

- Loading remote images in composables
- Configuring ImageLoader with cache, network fetching, and SVG support
- Adding authenticated or signed URL image support
- Creating custom image composable wrappers
- Debugging image loading failures or cache behavior
- Displaying user avatars or profile images
- Preloading images for smooth scrolling in lists
- Handling image loading errors with placeholder fallbacks

## Depends on

- **kmp-compose-ui** -- AsyncImage, setSingletonImageLoaderFactory, LocalPlatformContext
- **kmp-networking** -- Authorized Ktor HttpClient for image network requests
- **kmp-dependency-injection** -- Koin module for ImageLoader singleton
- **kmp-project-setup** -- Coil dependency declarations

## Workflow

1. Add Coil dependencies -- see [setup.md](references/setup.md)
2. Create imageLoaderModule with cache and component config -- see [image-loader-module.md](references/image-loader-module.md)
3. Implement ImageAuthInterceptor for signed URL resolution -- see [auth-interceptor.md](references/auth-interceptor.md)
4. Wire setSingletonImageLoaderFactory in App.kt -- see [integration.md](references/integration.md#app-kt-singleton-factory)
5. Register imageLoaderModule in commonModules after ktorfitModule -- see [integration.md](references/integration.md)

## Gotchas

1. **ImageLoader must be a singleton.** Multiple instances create separate memory and disk caches, doubling RAM usage. Always provide via Koin `single` and call `setSingletonImageLoaderFactory` exactly once at the top of the composition tree.
2. **KtorNetworkFetcherFactory shares the authorized HttpClient.** Image requests go through the auth interceptor chain. Ensure token refresh logic is thread-safe -- a concurrent image batch can trigger multiple refresh attempts.
3. **SVG decoder adds binary size.** If your app does not use SVGs, remove `SvgDecoder.Factory()` from components and drop the `coil-svg` dependency. The decoder pulls in an SVG parser that adds ~200KB to each platform binary.
4. **Disk cache must use SYSTEM_TEMPORARY_DIRECTORY.** On iOS, hardcoded paths crash because sandbox paths change between app installs. `FileSystem.SYSTEM_TEMPORARY_DIRECTORY` resolves to `NSTemporaryDirectory()` on iOS, which is the correct sandboxed location.
5. **crossfade doubles recomposition in scroll lists.** Each image triggers two compositions (placeholder then loaded). In `LazyColumn` with many images, disable crossfade or use `SubcomposeAsyncImage` with explicit loading slots to control recomposition cost.
6. **Memory cache percent is relative to total device RAM.** `maxSizePercent(0.25)` on an iOS device with 6GB RAM = 1.5GB cache ceiling. Consider `maxSizeBytes()` with an explicit cap if your app runs alongside memory-intensive features.
7. **Cache keys must be stable across sessions.** `urlToKey()` strips query parameters from signed URLs to produce a stable path-based key. Without the interceptor, every new token generates a new cache key, defeating disk cache entirely.
8. **setSingletonImageLoaderFactory must be called before any AsyncImage.** If an AsyncImage composable renders first, Coil creates a default loader without your interceptor, cache config, or Ktor fetcher -- and that default persists for the composition lifetime.
9. **Only diskCacheKey is overridden in the interceptor.** The interceptor sets `diskCacheKey` but not `memoryCacheKey`. If you need stable memory cache keys for signed URLs, add `.memoryCacheKey(cacheKey)` to the request builder in `buildRequest()`.
10. **println in onError is swallowed on iOS.** `println()` output does not appear in Xcode console by default. Use a shared logging abstraction (e.g., Napier, Kermit) so image load failures are visible during iOS debugging. See **kmp-logging** skill for structured logging patterns.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding Coil dependencies |
| [references/image-loader-module.md](references/image-loader-module.md) | Setting up ImageLoader and AppAsyncImage |
| [references/auth-interceptor.md](references/auth-interceptor.md) | Implementing signed URL resolution and retry |
| [references/integration.md](references/integration.md) | Wiring into App.kt and Koin module graph |
| [assets/snippets/image-loader-config.kt](assets/snippets/image-loader-config.kt) | Alternative configurations (public-only, custom cache sizes) |
| [assets/templates/custom-image-composable.kt.template](assets/templates/custom-image-composable.kt.template) | SubcomposeAsyncImage with loading/error states |

## Validation

### A. Build and compilation
- [ ] `@OptIn(ExperimentalCoilApi::class)` applied where `KtorNetworkFetcherFactory` is used
- [ ] Coil 3 artifact coordinates use `io.coil-kt.coil3` group (not `io.coil-kt`)
- [ ] Version catalog entries use `version.ref` syntax

### B. Image loading correctness
- [ ] Memory cache configured with `MemoryCache.Builder().maxSizePercent()`
- [ ] Disk cache configured with `DiskCache.Builder()` using `SYSTEM_TEMPORARY_DIRECTORY`
- [ ] Disk cache size has an explicit `maxSizeBytes` cap
- [ ] Placeholder and error states handled in composable wrappers
- [ ] Crossfade enabled in ImageLoader config
- [ ] SVG support included via `SvgDecoder.Factory()`
- [ ] No `android.*` imports in any commonMain file
- [ ] `ImageRequest.Builder` uses `LocalPlatformContext.current` (not hardcoded context)
- [ ] `diskCacheKey` set to stable value in interceptor (not signed URL with query params)
- [ ] Interceptor retries on 401/403 with fresh signed URL

### C. Performance
- [ ] ImageLoader is a Koin `single` (not `factory`)
- [ ] Ktor HttpClient reused from DI graph (not constructed per request)
- [ ] `urlToKey()` strips query params to prevent cache key churn
- [ ] No full-resolution images loaded into thumbnail-sized composables in examples

### D. Security
- [ ] Signed URL resolution goes through StorageService (not hardcoded tokens)
- [ ] `runCatching` used around network calls to prevent interceptor crashes
- [ ] No tokens or secrets logged in error handlers

### E. Integration
- [ ] `imageLoaderModule()` registered after `ktorfitModule()` in commonModules
- [ ] `setSingletonImageLoaderFactory` called before any AsyncImage in App.kt
- [ ] QUALIFIER_NAME_AUTHORIZATION_CLIENT constant used consistently (not string literal)
- [ ] Dependency graph in integration.md matches actual Koin wiring
