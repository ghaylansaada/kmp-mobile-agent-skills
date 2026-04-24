package {your.package}.snippets

import {your.package}.data.repository.AccountRepository
import {your.package}.domain.model.Account
import {your.package}.network.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * MockK patterns for coroutine-based code.
 *
 * GOTCHA: Use coEvery/coVerify for suspend functions, NOT every/verify.
 * Using every { } with a suspend function silently fails to record the
 * call and throws "Missing mocked calls inside every { ... }".
 *
 * GOTCHA: MockK only works in Android/JVM test source sets.
 * It depends on JVM bytecode manipulation and throws
 * ClassNotFoundException on iOS. Use hand-written fakes in commonTest.
 */
class MockKCoroutineSnippets {

    private val mockRepo = mockk<AccountRepository>(relaxed = true)

    // --- Stub a suspend function ---
    @Test
    fun stubSuspendFunction() = runTest {
        coEvery { mockRepo.fetchAccounts() } returns ApiResult.Success(emptyList())

        val result = mockRepo.fetchAccounts()

        coVerify { mockRepo.fetchAccounts() }
    }

    // --- Stub a Flow-returning function ---
    @Test
    fun stubFlowFunction() = runTest {
        every { mockRepo.observeAccounts() } returns flowOf(emptyList())

        // Use the flow in test...
    }

    // --- Capture arguments ---
    @Test
    fun captureArguments() = runTest {
        val usernameSlot = slot<String>()
        val passwordSlot = slot<String>()
        coEvery {
            mockRepo.login(capture(usernameSlot), capture(passwordSlot))
        } returns ApiResult.Success(mockk(relaxed = true))

        mockRepo.login("alice", "secret")

        assertEquals("alice", usernameSlot.captured)
        assertEquals("secret", passwordSlot.captured)
    }

    // --- Stub sequential returns ---
    @Test
    fun sequentialReturns() = runTest {
        coEvery { mockRepo.fetchAccounts() } returnsMany listOf(
            ApiResult.Error(code = 500, message = "First call fails"),
            ApiResult.Success(emptyList()),  // Second call succeeds
        )

        val first = mockRepo.fetchAccounts()
        val second = mockRepo.fetchAccounts()

        // first is Error, second is Success
    }

    // --- Verify call count ---
    @Test
    fun verifyCallCount() = runTest {
        coEvery { mockRepo.fetchAccounts() } returns ApiResult.Success(emptyList())

        mockRepo.fetchAccounts()
        mockRepo.fetchAccounts()

        coVerify(exactly = 2) { mockRepo.fetchAccounts() }
    }
}
