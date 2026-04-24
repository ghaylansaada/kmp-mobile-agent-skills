---
name: kmp-navigation
description: >
  Navigation 3 for Compose Multiplatform -- user-owned back stack with NavDisplay,
  @Serializable NavKey route definitions, scene strategies for adaptive multi-destination
  layouts, entry factories, polymorphic serialization for KMP non-JVM targets,
  auth-gated flows via SessionState, bottom navigation with tab state preservation,
  deep link handling, and Navigation 2 migration guidance. Activate when implementing
  screen navigation, defining routes, adding deep links, handling auth-gated flows,
  setting up bottom navigation, or migrating from Navigation 2.
compatibility: >
  KMP with Navigation 3 (androidx.navigation3). Requires Compose Multiplatform 1.10+.
metadata:
  domain: kotlin-multiplatform
  targets: android, ios
---

# KMP Navigation (Navigation 3)

## When to use

- Adding type-safe navigation with @Serializable NavKey route objects/classes
- Setting up NavDisplay with a user-owned back stack
- Implementing auth-gated flows reacting to SessionState
- Setting up bottom navigation with tabs and state preservation
- Using scene strategies for adaptive multi-destination layouts (list-detail on tablets)
- Handling deep links from external URIs
- Adding new feature screens or navigation entries
- Migration from Navigation 2 to Navigation 3

## Depends on

- **kmp-architecture** -- Kotlin Multiplatform project structure, expect/actual, targets
- **kmp-adaptive-ui** -- Adaptive layouts, window size classes, scene strategies for multi-destination display
- **kmp-dependency-injection** -- Koin for ViewModel injection in screens
- **kmp-session-management** -- SessionManager/SessionState for auth gating

## Workflow

1. Add Navigation 3 dependencies and configure polymorphic serialization --> [setup.md](references/setup.md)
2. Define NavKey routes and set up NavDisplay with user-owned back stack --> [routes-and-display.md](references/routes-and-display.md)
3. Build navigation patterns: bottom nav, nested navigation, argument passing --> [navigation-patterns.md](references/navigation-patterns.md)
4. Configure deep link handling for Android and iOS --> [deep-links.md](references/deep-links.md)
5. Implement auth-gated navigation with entry wrappers --> [auth-gates.md](references/auth-gates.md)
6. Migrate existing Navigation 2 code to Navigation 3 --> [migration.md](references/migration.md)
7. Verify navigation correctness across platforms --> Validation checklist below

## Gotchas

1. **KMP non-JVM targets need polymorphic serialization (not reflection).** iOS and Web targets cannot use reflection-based serialization. You must register a `SerializersModule` with polymorphic declarations for all NavKey subtypes, and pass it to `SavedStateConfiguration`. Without this, navigation state save/restore silently fails on non-JVM targets.
2. **Back stack is user-owned -- you must manage it explicitly.** Unlike Nav2 where NavController owns the back stack, Nav3 gives you a `SnapshotStateList<NavKey>` that you add to and remove from directly. Forgetting to remove entries means the user cannot navigate back.
3. **NavKey must be @Serializable.** Every route object/class implementing NavKey needs the `@Serializable` annotation. Missing it causes `SerializationException: Serializer for class 'YourKey' is not found`. The `kotlinSerialization` plugin must be applied in build.gradle.kts.
4. **Multi-destination display requires scene strategies.** To show multiple destinations simultaneously (e.g., list-detail on tablets), you must configure a scene strategy on NavDisplay. Without it, NavDisplay renders only the top-of-stack entry regardless of screen size.
5. **Deep link handling differs from Nav2.** Nav2 used NavDeepLink annotations and automatic intent matching. Nav3 requires you to manually parse incoming URIs and push the appropriate NavKey onto the back stack.
6. **NavDisplay is not NavHost.** NavDisplay does not manage a NavController or graph builder. You provide it a back stack and an entry factory (mapping keys to composables). Mixing Nav2 NavHost patterns with Nav3 will not compile.
7. **ViewModel scoping with Nav3 requires lifecycle-viewmodel-navigation3.** Use `rememberViewModelStoreNavEntryDecorator()` from the lifecycle-viewmodel-navigation3 artifact to scope ViewModels to individual navigation entries. Without this decorator, ViewModels are not properly scoped to destinations.
8. **Tab state is not automatically preserved.** Unlike Nav2's `saveState`/`restoreState` pattern, Nav3 requires you to manage separate back stacks per tab or use scene strategies to preserve tab state across switches.

## Assets

| Path | Load when... |
|------|-------------|
| [references/setup.md](references/setup.md) | Adding Navigation 3 dependencies |
| [references/routes-and-display.md](references/routes-and-display.md) | Defining NavKey routes and NavDisplay |
| [references/navigation-patterns.md](references/navigation-patterns.md) | Bottom nav, nested navigation, argument passing |
| [references/deep-links.md](references/deep-links.md) | Handling deep links on Android and iOS |
| [references/auth-gates.md](references/auth-gates.md) | Auth-gated navigation with entry wrappers |
| [references/migration.md](references/migration.md) | Migrating from Navigation 2 to Navigation 3 |
| [assets/snippets/navigate-with-args.kt](assets/snippets/navigate-with-args.kt) | Nav3 navigation patterns and back stack utilities |
| [assets/templates/feature-nav-graph.kt.template](assets/templates/feature-nav-graph.kt.template) | Feature entry factory scaffold with list/detail pattern |

## Validation

### A. Build and Compilation

- [ ] All imports resolve against declared Nav3 dependencies (no Nav2 artifacts)
- [ ] No deprecated Nav2 API usage (NavHost, NavController, composable builder, string-based routes)
- [ ] Polymorphic serialization module registered for all NavKey subtypes on non-JVM targets

### B. Navigation Correctness

- [ ] All routes implement NavKey and use `@Serializable` annotation
- [ ] Back stack is a `SnapshotStateList<NavKey>` created with `rememberNavBackStack()`
- [ ] NavDisplay used with entry factory (not NavHost with graph builder)
- [ ] Back stack explicitly managed: add for forward navigation, removeLastOrNull for back
- [ ] Auth state transitions clear back stack and push appropriate starting key
- [ ] Deep link handling parses URIs and pushes NavKey onto back stack
- [ ] Scene strategies configured for adaptive multi-destination layouts
- [ ] No `android.*` or `androidx.activity.*` imports in commonMain code
- [ ] Navigation callbacks passed as lambdas to screens (screens do not hold back stack reference)
- [ ] Bottom nav manages per-tab state correctly

### C. Security

- [ ] No hardcoded API keys, tokens, or secrets in route definitions or navigation code
- [ ] Deep link scheme/host do not expose internal endpoints

### D. Performance

- [ ] NavDisplay uses stable keys to avoid unnecessary recomposition
- [ ] ViewModel scoping matches navigation entry lifecycle via viewmodel-navigation3 decorator
- [ ] Bottom nav avoids recreating tab destinations on every switch

### E. Integration

- [ ] Depends-on references match actual skill directory names
- [ ] SessionManager integration documented with correct Flow collection
- [ ] Koin injection pattern (`koinInject()`) consistent with kmp-dependency-injection
- [ ] App.kt migration from direct screen call to AppNavDisplay documented
- [ ] Connected skills list accurate and bidirectional
