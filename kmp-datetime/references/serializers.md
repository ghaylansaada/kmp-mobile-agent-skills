# ISO 8601 Serializers

kotlinx-datetime provides built-in `Instant.serializer()`. These custom serializers are for field-level `@Serializable(with = ...)` or lenient parsing:

```kotlin
package {your.package}.core.datetime

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantIso8601Serializer : KSerializer<Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor("InstantISO8601", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant =
        Instant.parse(decoder.decodeString())
}

object LocalDateIso8601Serializer : KSerializer<LocalDate> {
    override val descriptor =
        PrimitiveSerialDescriptor("LocalDateISO8601", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate.parse(decoder.decodeString())
}

object LocalDateTimeIso8601Serializer : KSerializer<LocalDateTime> {
    override val descriptor =
        PrimitiveSerialDescriptor("LocalDateTimeISO8601", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString())
}
```

## LenientInstantSerializer

Normalizes non-standard server formats (space instead of T, missing Z, microseconds):

```kotlin
object LenientInstantSerializer : KSerializer<Instant> {
    override val descriptor =
        PrimitiveSerialDescriptor("LenientInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        val raw = decoder.decodeString()
            .replace(" ", "T")
            .let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it }
        return Instant.parse(raw)
    }
}
```

## Example DTO Usage

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("created_at")
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
    @SerialName("event_date")
    @Serializable(with = LocalDateIso8601Serializer::class)
    val eventDate: LocalDate,
)
```
