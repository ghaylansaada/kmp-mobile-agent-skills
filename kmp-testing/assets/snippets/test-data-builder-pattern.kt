package {your.package}.snippets

/**
 * Test data builder pattern.
 *
 * Use top-level factory functions with default parameters. Each test
 * overrides only the fields relevant to that scenario, keeping tests
 * readable and resilient to constructor changes.
 *
 * GOTCHA: When the production data class adds a new field, you add a
 * default value here ONCE and all existing tests continue to compile.
 * If you constructed data classes inline in each test, every test
 * would need updating.
 */

// --- Domain model (production code) ---
data class Account(
    val id: String,
    val username: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val isActive: Boolean,
)

// --- Builder function (test code) ---
fun buildAccount(
    id: String = "default-id",
    username: String = "defaultuser",
    email: String = "default@example.com",
    displayName: String = "Default User",
    avatarUrl: String? = null,
    isActive: Boolean = true,
): Account = Account(
    id = id,
    username = username,
    email = email,
    displayName = displayName,
    avatarUrl = avatarUrl,
    isActive = isActive,
)

// --- Usage in tests ---
// Only override what matters for this test:
// val activeAccount = buildAccount(isActive = true)
// val inactiveAccount = buildAccount(isActive = false, displayName = "Banned User")
// val listOfAccounts = (1..10).map { buildAccount(id = "id-$it") }
