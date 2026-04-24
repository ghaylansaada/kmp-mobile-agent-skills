# Android Implementation: BiometricPrompt + Keystore

## BiometricAuthenticator -- actual

**File:** `androidMain/.../core/biometrics/BiometricAuthenticator.android.kt`

```kotlin
package {your.package}.core.biometrics

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import {your.package}.core.platform.PlatformContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class BiometricAuthenticator actual constructor(
    private val context: PlatformContext,
) {
    private val biometricManager = BiometricManager.from(context.applicationContext)

    actual fun canAuthenticate(): Boolean =
        biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    actual fun canAuthenticateWithDeviceCredential(): Boolean {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL
        } else {
            Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
        }
        return biometricManager.canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        allowDeviceCredential: Boolean,
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val activity = context.currentActivity as? FragmentActivity ?: run {
            continuation.resume(BiometricResult.Failure("No FragmentActivity available"))
            return@suspendCancellableCoroutine
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                if (continuation.isActive) {
                    continuation.resume(BiometricResult.Success)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                continuation.resume(
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        -> BiometricResult.Cancelled
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        -> BiometricResult.NotEnrolled
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE,
                        -> BiometricResult.NotAvailable
                        else -> BiometricResult.Failure(errString.toString())
                    },
                )
            }
            // NOT terminal -- user can retry. Do NOT resume here.
            override fun onAuthenticationFailed() = Unit
        }

        val executor = ContextCompat.getMainExecutor(context.applicationContext)
        val prompt = BiometricPrompt(activity, executor, callback)

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            infoBuilder.setAllowedAuthenticators(
                Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
            )
        } else {
            // API < 30: setNegativeButtonText and DEVICE_CREDENTIAL are mutually exclusive
            infoBuilder.setNegativeButtonText(context.getString(Res.string.action_cancel))
            infoBuilder.setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
        }

        activity.runOnUiThread { prompt.authenticate(infoBuilder.build()) }
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }
}
```

## SecureCredentialStore -- actual (Keystore-backed)

**File:** `androidMain/.../core/biometrics/SecureCredentialStore.android.kt`

```kotlin
package {your.package}.core.biometrics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import {your.package}.core.platform.PlatformContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual class SecureCredentialStore actual constructor(
    private val context: PlatformContext,
) {
    private val prefs = context.applicationContext
        .getSharedPreferences("secure_credentials", Context.MODE_PRIVATE)

    private val keyStore = KeyStore.getInstance("AndroidKeyStore")
        .apply { load(null) }

    actual fun store(key: String, value: String) {
        val secretKey = getOrCreateKey(key)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val combined = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(key, Base64.encodeToString(combined, Base64.NO_WRAP))
            .apply()
    }

    actual fun retrieve(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.sliceArray(0 until 12)
            val encrypted = combined.sliceArray(12 until combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(key),
                GCMParameterSpec(128, iv),
            )
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Key invalidated by biometric enrollment change -- delete stale data
            delete(key)
            null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    actual fun delete(key: String) {
        prefs.edit().remove(key).apply()
        val alias = "credential_$key"
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    actual fun clear() {
        prefs.all.keys.toList().forEach { delete(it) }
    }

    private fun getOrCreateKey(key: String): SecretKey {
        val alias = "credential_$key"
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
```
