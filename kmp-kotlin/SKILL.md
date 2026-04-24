---
name: kmp-kotlin
description: >
  Kotlin 2.3.20+ language conventions, best practices, and modern idioms.
  Covers naming, formatting, KDoc, type safety, null safety, modern syntax
  (guard conditions, context parameters, multi-dollar interpolation, explicit
  backing fields), error handling, and anti-patterns.
  Activate when writing new Kotlin code, reviewing code style, or enforcing
  language-level conventions. Includes awareness of Kotlin 2.4 features.
  Does NOT cover coroutine patterns -- see kmp-kotlin-coroutines skill.
compatibility: >
  Kotlin 2.3.20+. Context parameters coverage includes 2.4-stable preview.
metadata:
  domain: kotlin
  targets: android, ios, jvm
---

# KMP Kotlin Conventions

## Scope

Single source of truth for Kotlin language conventions targeting Kotlin 2.3.20 and later. Covers naming, formatting, KDoc, type safety, null safety, modern syntax (including features stabilized in 2.1 through 2.3 and previewed for 2.4), error handling, and anti-patterns. Does not cover coroutine patterns (see **kmp-kotlin-coroutines** skill), KMP-specific patterns (expect/actual, source sets), architecture (ViewModel, Repository), or framework APIs (Compose, Ktor, Koin).

## When to use

- Writing new Kotlin code in any source set
- Reviewing code style or enforcing language-level conventions
- Flagging anti-patterns during code review
- Deciding between language constructs (sealed interface vs enum, value class vs data class, etc.)
- Adopting Kotlin 2.2+ features (guard conditions, non-local break/continue, multi-dollar interpolation)
- Applying Kotlin error handling patterns (preconditions, Result, sealed errors)

## Depends on

None -- this is a language-level skill with no framework dependencies.

## Workflow

1. Review naming and formatting conventions --> [references/naming-and-formatting.md](references/naming-and-formatting.md)
2. Apply type safety patterns --> [references/type-safety.md](references/type-safety.md)
3. Apply null safety patterns --> [references/null-safety.md](references/null-safety.md)
4. Apply modern syntax and language features --> [references/modern-syntax.md](references/modern-syntax.md)
5. Apply error handling patterns --> [references/error-handling.md](references/error-handling.md)
6. Review anti-patterns checklist --> [references/anti-patterns.md](references/anti-patterns.md)

## Gotchas

1. **Guard conditions use `if`, not `&&`.** Write `is String if x.length > 5 ->`, not `is String && x.length > 5 ->`. The `if` keyword is required syntax, not a stylistic choice.
2. **Context parameters resolve by TYPE, not by name.** Two context parameters of the same type (e.g., two `Logger` instances) cause ambiguity errors at the call site. Use wrapper types or `@JvmInline value class` to differentiate.
3. **`inline fun` copies bytecode at every call site.** Large inline functions bloat the binary. Only inline functions that accept lambda parameters, use reified types, or are on a measured hot path.
4. **`by lazy` is thread-safe by default (synchronized).** In single-threaded contexts (e.g., UI thread only), pass `LazyThreadSafetyMode.NONE` to avoid unnecessary synchronization overhead.
5. **`data class` `copy()` is shallow.** Nested mutable objects inside a copied data class share references with the original. Mutating a nested list in the copy mutates the original.
6. **`runCatching` catches ALL exceptions including `CancellationException`.** This is dangerous in suspend functions -- see **kmp-kotlin-coroutines** skill for the `suspendRunCatching` pattern and full coroutine error handling.
7. **`sealed class` forces an open constructor.** Use `sealed interface` unless subtypes genuinely need shared mutable state in the base type. Sealed interfaces allow subtypes to extend other classes.
8. **`value class` boxes in generic contexts.** When a `@JvmInline value class` is used as a type argument (`List<UserId>`), the JVM boxes it, negating the allocation benefit.
9. **Smart casts fail on mutable properties and custom getters.** `var` properties can change between the null check and usage. Properties with custom `get()` are re-evaluated. Assign to a local `val` first.
10. **`when` without `else` only compiles for sealed/enum subjects.** For `Int`, `String`, or other open types, the compiler requires an `else` branch. Conversely, adding `else` to a sealed `when` defeats exhaustiveness checking.
11. **Multi-dollar interpolation requires matching dollar count.** In `$$"template"`, variables need `$$var` -- a single `$var` is treated as literal text. The dollar count in the prefix must match the dollar count on variable references.
12. **Name-based destructuring (Kotlin 2.3.20 preview) is positional by default.** You must explicitly use `val name = componentN` syntax to get name-based matching. Mixing positional and name-based destructuring in the same declaration is not allowed.
13. **Non-local `break`/`continue` only works in inline lambdas.** Using `break` or `continue` inside a non-inline lambda (e.g., `forEach` on a `Sequence`) is still a compile error.
14. **`Delegates.observable` fires AFTER the value changes.** The callback receives the old and new values, but the property already holds the new value. Use `Delegates.vetoable` to intercept and reject changes before they apply.
15. **Extension functions are resolved statically.** An extension on a base type is called even when the runtime type is a subtype. Extension functions do not support polymorphic dispatch -- use member functions for polymorphism.

## Assets

| Path | Load when... |
|------|-------------|
| [references/naming-and-formatting.md](references/naming-and-formatting.md) | Checking naming conventions, KDoc format, file naming, or code formatting rules |
| [references/type-safety.md](references/type-safety.md) | Choosing between sealed interface, enum, value class, or data class; working with generics, variance, or reified types |
| [references/null-safety.md](references/null-safety.md) | Handling nullable types, platform types, smart casts, or contracts |
| [references/modern-syntax.md](references/modern-syntax.md) | Using scope functions, collections, extension functions, delegation, DSLs, inline functions, or Kotlin 2.2+ features |
| [references/error-handling.md](references/error-handling.md) | Implementing error handling, preconditions, Result types, or sealed result hierarchies |
| [references/anti-patterns.md](references/anti-patterns.md) | Reviewing code for common Kotlin anti-patterns |

## Validation

### A. Naming and formatting
- [ ] All public classes, functions, and properties have KDoc with `@param`, `@return`, `@throws` as applicable
- [ ] PascalCase for classes/interfaces/objects, camelCase for functions/properties, SCREAMING_CASE for constants and enum entries
- [ ] No Hungarian notation (`mVariable`, `sInstance`)
- [ ] File names: PascalCase matching the top-level class, lowercase for utility files without a dominant class
- [ ] 4-space indentation, no tabs, 120-character line limit
- [ ] Explicit imports only -- no wildcard `*` imports
- [ ] Trailing commas on all multi-line parameter lists, argument lists, and collection literals
- [ ] No double blank lines; one blank line between top-level declarations

### B. Type safety
- [ ] `sealed interface` used over `sealed class` for state/result/error hierarchies (unless shared mutable state is needed)
- [ ] `@JvmInline value class` used for type-safe wrappers (IDs, tokens, amounts)
- [ ] `when` expressions on sealed/enum types are exhaustive with no `else` branch
- [ ] `typealias` used only for complex generic signatures, not as a substitute for value classes
- [ ] Generic constraints use `where` clause when multiple bounds are needed
- [ ] Variance annotations (`in`/`out`) applied correctly on generic type parameters

### C. Null safety
- [ ] No `!!` without a documented justification comment on the same or preceding line
- [ ] Platform types from Java interop are explicitly annotated as nullable (`: Type?`)
- [ ] `requireNotNull` for input validation, `checkNotNull` for state validation
- [ ] Smart cast limitations handled (mutable properties assigned to local `val` before null check)
- [ ] `filterNotNull()`, `mapNotNull()`, `firstNotNullOf()` used instead of manual null filtering

### D. Modern syntax
- [ ] `when` preferred over if-else chains for multi-branch logic
- [ ] Expression body (`=`) used for single-expression functions
- [ ] `val` preferred over `var` everywhere possible
- [ ] Immutable collections in public APIs; `buildList`/`buildMap`/`buildSet` for conditional construction
- [ ] `Sequence` used for multi-step transformations on large collections
- [ ] Guard conditions (Kotlin 2.2+) used where they simplify `when` branches
- [ ] Scope functions used appropriately (no nesting beyond one level)
- [ ] `@Deprecated` annotations include `ReplaceWith` for IDE-assisted migration

### E. Error handling
- [ ] `require`/`check` used for preconditions instead of manual `if + throw`
- [ ] `runCatching` in suspend functions rethrows `CancellationException`
- [ ] Exception catches are specific -- no broad `catch (e: Exception)` or `catch (e: Throwable)`
- [ ] `use {}` used for `Closeable`/`AutoCloseable` resources
- [ ] Domain errors modeled as sealed interface hierarchies when callers need exhaustive handling

### F. Anti-patterns
- [ ] No `var` where `val` suffices
- [ ] No mutable collections in public APIs
- [ ] No `Any` as a parameter type
- [ ] No `lateinit` where nullable, lazy, or constructor injection works
- [ ] No raw string comparisons that should be enums or sealed types
- [ ] No string concatenation with `+` -- use string templates
- [ ] No `GlobalScope` usage
- [ ] No magic numbers or hardcoded strings -- extract to named constants
