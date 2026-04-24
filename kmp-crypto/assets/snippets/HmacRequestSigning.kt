package {your.package}.crypto

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.util.date.GMTDate

/**
 * Ktor client plugin that signs outgoing requests with HMAC-SHA256.
 *
 * Adds the following headers to each request:
 *   X-Api-Key:     the public API key
 *   X-Timestamp:   Unix timestamp in seconds
 *   X-Nonce:       random 16-byte hex nonce
 *   X-Signature:   HMAC-SHA256(secretKey, signable_string)
 *
 * The signable string is: METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_HASH
 *
 * Usage:
 *   val client = HttpClient(engineFactory) {
 *       install(HmacSigningPlugin) {
 *           apiKey = BuildConfig.API_KEY       // Never hardcode
 *           secretKey = BuildConfig.HMAC_SECRET // Never hardcode
 *           cryptoProvider = get<CryptoProvider>()
 *       }
 *   }
 *
 * Gotcha: The nonce must be unique per request. This implementation uses
 * SecureRandomProvider (cryptographically secure). Do NOT replace with
 * kotlin.random.Random.Default which is NOT cryptographically secure.
 */
class HmacSigningConfig {
    var apiKey: String = ""
    var secretKey: String = ""
    lateinit var cryptoProvider: CryptoProvider
}

val HmacSigningPlugin = createClientPlugin(
    "HmacSigningPlugin",
    ::HmacSigningConfig,
) {
    val apiKey = pluginConfig.apiKey
    val secretKey = pluginConfig.secretKey
    val crypto = pluginConfig.cryptoProvider

    on(Send) { request ->
        val timestamp = (GMTDate().timestamp / 1000).toString()
        val nonce = crypto.randomHex(16)

        // Hash the request body (empty string hash if no body)
        val bodyContent = request.body as? OutgoingContent
        val bodyString = when (bodyContent) {
            is TextContent -> bodyContent.text
            else -> ""
        }
        val bodyHash = crypto.sha256Hex(bodyString)

        // Build the signable string using path only (not full URL)
        val method = request.method.value.uppercase()
        val path = request.url.encodedPath
        val signableString = "$method\n$path\n$timestamp\n$nonce\n$bodyHash"

        // Compute HMAC signature
        val signature = crypto.hmacSha256Hex(secretKey, signableString)

        // Add auth headers
        request.header("X-Api-Key", apiKey)
        request.header("X-Timestamp", timestamp)
        request.header("X-Nonce", nonce)
        request.header("X-Signature", signature)

        proceed(request)
    }
}
