# ProGuard/R8 Rules for the KMP Stack

## Overview

Place the rules template at `composeApp/proguard-rules.pro`. Copy from
[assets/templates/proguard-rules.pro](../assets/templates/proguard-rules.pro) and replace
`{your.package}` with your actual package name.

The template covers: Kotlin core, kotlinx.serialization, Ktor, Ktorfit, Koin, Room, Coil,
and Compose. Every library that uses reflection, code generation, or `ServiceLoader` needs
explicit keep rules -- the default Android ProGuard config does not cover them.

## Why each library needs rules

| Library | R8 behavior without rules | Runtime failure |
|---------|--------------------------|-----------------|
| kotlinx.serialization | Strips `$$serializer` classes | `ClassNotFoundException` on deserialization |
| Ktor / Ktorfit | Strips engine and generated `_KtorfitImpl` classes | Network calls crash or return nothing |
| Koin | Removes "unused" DI-registered classes | `NoBeanDefFoundException` |
| Room | Strips `_Impl` database/DAO classes | "Cannot find implementation" crash |
| Coil | Strips `ServiceLoader`-discovered components | Images silently fail to load |

## R8 full mode (optional)

For maximum optimization, add to `gradle.properties`:

```properties
android.enableR8.fullMode=true
```

Full mode is more aggressive and may require additional keep rules. It can break libraries
that rely on default constructor preservation or enum values accessed by name. Test
thoroughly before enabling.

## Adding rules for new libraries

When adding a library that uses reflection or code generation:

1. Check the library's documentation for recommended ProGuard rules
2. If none exist, identify the generated or reflectively-accessed classes
3. Add a `-keep` rule for those classes in `composeApp/proguard-rules.pro`
4. Build a release APK and test the affected feature on a real device

## Debugging stripped classes

When a release build crashes with `ClassNotFoundException` or `NoSuchMethodError`:

1. Read the class name from the stack trace
2. Check `composeApp/build/outputs/mapping/release/mapping.txt` to see if it was renamed or removed
3. Add a targeted keep rule:
   ```proguard
   -keep class <fully.qualified.ClassName> { *; }
   ```
4. Rebuild and test
