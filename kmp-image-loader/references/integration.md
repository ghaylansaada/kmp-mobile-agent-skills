# Image Loader Integration

## Upstream Dependencies

| Skill | Provides |
|-------|----------|
| kmp-compose-ui | AsyncImage, setSingletonImageLoaderFactory, LocalPlatformContext |
| kmp-networking | Authorized Ktor HttpClient for image network requests |
| kmp-dependency-injection | Koin module registration, PlatformContext singleton |
| kmp-project-setup | Coil dependency declarations |

## Koin Module Registration Order

```kotlin
fun commonModules(context: PlatformContext) = listOf(
    coreModule(context),
    localStorageModule(),
    sessionModule(),
    ktorfitModule(),
    externalStorageModule(),
    imageLoaderModule(),       // Must come after ktorfitModule
    repositoryModule(),
    viewModelModule()
)
```

## Dependency Graph

```
imageLoaderModule
    +-- ImageAuthInterceptor → StorageService (from ktorfitModule)
    +-- ImageLoader
            +-- PlatformContext (from coreModule)
            +-- ImageAuthInterceptor
            +-- HttpClient named "authorizedHttpClient" (from ktorfitModule)
```

## App.kt Singleton Factory

```kotlin
@Composable
fun App() {
    val imageLoader = koinInject<ImageLoader>()
    setSingletonImageLoaderFactory { imageLoader }

    MaterialTheme {
        ScreenContent()  // AsyncImage renders after factory is set
    }
}
```

`setSingletonImageLoaderFactory` must be called once at the top of the composition tree, before any AsyncImage renders. If called after, Coil creates a default ImageLoader without the custom interceptor and cache configuration.

## Connected Skills

- **kmp-resources-management**: For local drawable resources, use `painterResource()` instead of AsyncImage
- **kmp-networking**: ImageAuthInterceptor handles 401/403 internally; only persistent errors surface to `onError`
