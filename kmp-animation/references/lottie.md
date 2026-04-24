# Lottie Animations with Compottie

[Compottie](https://github.com/nicholasgasior/compottie) is a Compose Multiplatform Lottie player that renders After Effects animations exported as JSON. It works on both Android and iOS without platform-specific code.

## Setup

Add the Compottie dependency to the version catalog and build configuration. Do not hardcode the version in build.gradle.kts.

**gradle/libs.versions.toml:**
```toml
[versions]
compottie = "<latest-version>"  # Check GitHub for current version

[libraries]
compottie = { module = "io.github.nicholasgasior.compottie:compottie", version.ref = "compottie" }
compottie-resources = { module = "io.github.nicholasgasior.compottie:compottie-resources", version.ref = "compottie" }
```

**shared/build.gradle.kts:**
```kotlin
commonMain.dependencies {
    implementation(libs.compottie)
    implementation(libs.compottie.resources)  // For loading from Compose resources
}
```

## Loading Compositions

### From Compose Resources

Place `.json` Lottie files in `composeResources/files/`:

```
composeApp/
  src/
    commonMain/
      composeResources/
        files/
          loading.json
          success.json
          empty-state.json
```

```kotlin
@Composable
fun LottieFromResources() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/loading.json").decodeToString()
        )
    }

    LottieAnimation(
        composition = composition,
        modifier = Modifier.size(AppTheme.sizing.lottieDefault),
    )
}
```

### From JSON String

```kotlin
@Composable
fun LottieFromString(jsonString: String) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(jsonString)
    }

    LottieAnimation(
        composition = composition,
        modifier = Modifier.size(AppTheme.sizing.lottieDefault),
    )
}
```

## Automatic Playback with animateLottieCompositionAsState

The simplest way to play a Lottie animation. It automatically animates progress from 0 to 1.

```kotlin
@Composable
fun AutoPlayLottie() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/loading.json").decodeToString()
        )
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,  // Loop forever
        speed = 1f,
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(AppTheme.sizing.lottieSmall),
    )
}
```

### Playback Parameters

```kotlin
animateLottieCompositionAsState(
    composition = composition,
    isPlaying = true,           // Pause/resume
    speed = 1.5f,               // 1.5x speed
    iterations = 3,             // Play 3 times then stop
    reverseOnRepeat = true,     // Play forward then backward
    clipSpec = LottieClipSpec.Progress(
        min = 0.2f,             // Start at 20%
        max = 0.8f,             // End at 80%
    ),
)
```

## Imperative Control with LottieAnimatable

For full control over playback (start, pause, seek, reverse):

```kotlin
@Composable
fun ControlledLottie() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/success.json").decodeToString()
        )
    }
    val animatable = rememberLottieAnimatable()

    // Play once when composition loads
    LaunchedEffect(composition) {
        composition?.let {
            animatable.animate(
                composition = it,
                iterations = 1,
                speed = 1f,
            )
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { animatable.progress },
        modifier = Modifier.size(AppTheme.sizing.lottieDefault),
    )
}
```

### Play on Event (Success/Error)

```kotlin
@Composable
fun SubmitButton(
    onSubmit: suspend () -> Boolean,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(SubmitState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    val successComposition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/success.json").decodeToString()
        )
    }
    val animatable = rememberLottieAnimatable()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            SubmitState.Idle -> {
                Button(onClick = {
                    state = SubmitState.Loading
                    coroutineScope.launch {
                        val success = onSubmit()
                        state = if (success) SubmitState.Success else SubmitState.Error
                    }
                }) {
                    Text(stringResource(Res.string.action_submit)) // Res.string.action_submit
                }
            }
            SubmitState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(AppTheme.sizing.iconMd))
            }
            SubmitState.Success -> {
                LaunchedEffect(successComposition) {
                    successComposition?.let {
                        animatable.animate(composition = it, iterations = 1)
                        // After animation completes, reset
                        state = SubmitState.Idle
                    }
                }
                LottieAnimation(
                    composition = successComposition,
                    progress = { animatable.progress },
                    modifier = Modifier.size(AppTheme.sizing.iconXl),
                )
            }
            SubmitState.Error -> {
                Text(
                    text = stringResource(Res.string.error_tap_to_retry), // Res.string.error_tap_to_retry
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { state = SubmitState.Idle },
                )
            }
        }
    }
}

private enum class SubmitState { Idle, Loading, Success, Error }
```

## Loading State Animation

```kotlin
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/loading.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(AppTheme.sizing.lottieMedium)
                    .semantics { contentDescription = stringResource(Res.string.cd_loading) },
            )
            Spacer(Modifier.height(AppTheme.spacing.lg))
            Text(
                text = stringResource(Res.string.label_loading), // Res.string.label_loading
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

## Empty State Animation

```kotlin
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    actionLabel: String = stringResource(Res.string.action_retry), // Res.string.action_retry
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/empty-state.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.5f,  // Slow, gentle loop
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(AppTheme.sizing.lottieDefault)
                .semantics { contentDescription = stringResource(Res.string.cd_no_content) },
        )
        Spacer(Modifier.height(AppTheme.spacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (onAction != null) {
            Spacer(Modifier.height(AppTheme.spacing.lg))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
```

## Memory Management

1. **Compositions are cached by `rememberLottieComposition`.** Re-entering a composable with the same spec reuses the parsed composition without re-parsing.

2. **Animations stop when the composable leaves composition.** Both `animateLottieCompositionAsState` and `LottieAnimatable` are lifecycle-aware and stop frame updates automatically.

3. **Large Lottie files increase memory usage.** Complex animations with many layers, masks, or high-resolution images embedded in the JSON can consume significant memory. Profile memory usage on iOS especially, as Skia rendering has higher overhead.

4. **Dispose pattern for conditional visibility:**
```kotlin
// The composition is released when AnimatedVisibility hides the content
AnimatedVisibility(visible = isLoading) {
    LottieLoadingIndicator()  // Composition auto-disposed when hidden
}
```

5. **Avoid loading multiple large compositions simultaneously.** If you have onboarding with multiple Lottie pages, load compositions lazily as each page becomes visible rather than all at once.
