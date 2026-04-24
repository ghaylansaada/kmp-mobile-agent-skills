# =====================================================================
# ProGuard / R8 rules -- security-specific additions
#
# The base KMP stack rules (Room, Ktor, Koin, Ktorfit, serialization,
# Coil, Compose) live in:
#   kmp-release/assets/templates/proguard-rules.pro
# Add THOSE first, then append these security-specific rules.
# =====================================================================

# -- SQLCipher -------------------------------------------------------------
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# -- AndroidX Security (EncryptedSharedPreferences) ----------------------
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# -- Security module -------------------------------------------------------
# Prevent obfuscation of security classes that use reflection/JNI
-keep class {your.package}.security.** { *; }
-keep class {your.package}.core.security.** { *; }
