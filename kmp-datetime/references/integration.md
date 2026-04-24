# Integration: DateTime

## Ktor API Serialization

Per-field serializers via `@Serializable(with = ...)`:

```kotlin
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserResponseDto(
    val id: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
)
```

Or configure module-level contextual serialization:

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val json = Json {
    serializersModule = SerializersModule {
        contextual(kotlinx.datetime.Instant::class, InstantIso8601Serializer)
    }
}
```

## Compose Display

Obtain formatters via constructor parameter or `remember`. This ensures `Clock` injection is consistent and avoids recreating expensive formatter objects:

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

@Composable
fun FormattedDate(
    instant: Instant,
    formatter: PlatformDateTimeFormatter,
    tz: TimeZone = TimeZone.currentSystemDefault(),
) {
    Text(text = formatter.formatInstant(instant, DateTimePatterns.DATE_LONG, tz))
}

@Composable
fun RelativeTime(
    instant: Instant,
    relativeFormatter: RelativeTimeFormatter,
) {
    Text(text = relativeFormatter.format(instant))
}
```

**Why `TimeZone` as a parameter on `FormattedDate`?** The composable sits at the UI boundary where `currentSystemDefault()` is appropriate as a default. But passing it explicitly allows previews and tests to use a fixed timezone.
