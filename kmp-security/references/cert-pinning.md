# Certificate Pinning

TLS certificate pinning for Android (OkHttp) and iOS (URLSession).

## commonMain -- CertificatePinningConfig.kt

Generate pin hashes:
```bash
openssl s_client -connect api.example.com:443 </dev/null 2>/dev/null |
  openssl x509 -pubkey -noout | openssl pkey -pubin -outform der |
  openssl dgst -sha256 -binary | openssl enc -base64
```

Always include a backup pin from a different CA or next certificate.

```kotlin
package {your.package}.security

data class CertificatePin(
    val hostname: String,
    val sha256Pins: List<String>,  // Base64-encoded SHA-256 SPKI hashes
)

object CertificatePinningConfig {
    val pins: List<CertificatePin> = listOf(
        CertificatePin(
            hostname = "api.example.com",
            sha256Pins = listOf(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // Primary (CA-1)
                "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=",  // Backup (CA-2)
            ),
        ),
    )
}
```

## androidMain -- CertificatePinningEngine.kt

The `sha256/` prefix is REQUIRED by OkHttp CertificatePinner. Omitting it causes
a silent no-op -- requests succeed without pinning.

```kotlin
package {your.package}.security

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.CertificatePinner

fun HttpClientConfig<OkHttpConfig>.installCertificatePinning(
    pins: List<CertificatePin> = CertificatePinningConfig.pins,
) {
    engine {
        val pinnerBuilder = CertificatePinner.Builder()
        pins.forEach { pin ->
            pin.sha256Pins.forEach { hash ->
                pinnerBuilder.add(pin.hostname, "sha256/$hash")
            }
        }
        config { certificatePinner(pinnerBuilder.build()) }
    }
}
```

## iosMain -- CertificatePinningEngine.kt

iOS ATS provides baseline TLS enforcement but does NOT do certificate pinning.
This delegate extracts the leaf certificate SPKI hash and compares it against
the configured pins. Without hash comparison, pinning is not enforced.

```kotlin
package {your.package}.security

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.Foundation.*
import platform.Security.*

@OptIn(ExperimentalForeignApi::class)
fun HttpClientConfig<DarwinClientEngineConfig>.installCertificatePinning(
    pins: List<CertificatePin> = CertificatePinningConfig.pins,
) {
    engine {
        handleChallenge { _, _, challenge, completionHandler ->
            val cancel = { completionHandler(
                NSURLSessionAuthChallengeDisposition
                    .NSURLSessionAuthChallengeCancelAuthenticationChallenge, null,
            ) }
            val serverTrust = challenge.protectionSpace.serverTrust
                ?: return@handleChallenge cancel()
            val host = challenge.protectionSpace.host
            val expectedPins = pins.firstOrNull { it.hostname == host }
            if (expectedPins == null) {
                completionHandler(
                    NSURLSessionAuthChallengeDisposition
                        .NSURLSessionAuthChallengePerformDefaultHandling, null,
                )
                return@handleChallenge
            }
            if (!SecTrustEvaluateWithError(serverTrust, null)) return@handleChallenge cancel()

            val certChain = SecTrustCopyCertificateChain(serverTrust)
            if (((certChain as? NSArray)?.count ?: 0uL) == 0uL) return@handleChallenge cancel()
            val leafCert = (certChain as NSArray).objectAtIndex(0u)
            val publicKey = SecCertificateCopyKey(leafCert as SecCertificateRef)
                ?: return@handleChallenge cancel()
            val publicKeyData = SecKeyCopyExternalRepresentation(publicKey, null)
                ?: return@handleChallenge cancel()

            val keyLength = CFDataGetLength(publicKeyData).toInt()
            val keyBytes = ByteArray(keyLength)
            keyBytes.usePinned { pinned ->
                CFDataGetBytePtr(publicKeyData)?.let { src ->
                    platform.posix.memcpy(pinned.addressOf(0), src, keyLength.toULong())
                }
            }
            val serverPin = keyBytes.toNSData().sha256()
                .base64EncodedStringWithOptions(0u)

            if (expectedPins.sha256Pins.contains(serverPin)) {
                val credential = NSURLCredential.credentialForTrust(serverTrust)
                completionHandler(
                    NSURLSessionAuthChallengeDisposition
                        .NSURLSessionAuthChallengeUseCredential, credential,
                )
            } else {
                cancel()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.sha256(): NSData {
    val hash = ByteArray(32) // CC_SHA256_DIGEST_LENGTH
    hash.usePinned { pinned ->
        platform.CoreCrypto.CC_SHA256(bytes, length.toUInt(), pinned.addressOf(0))
    }
    return hash.toNSData()
}
```
