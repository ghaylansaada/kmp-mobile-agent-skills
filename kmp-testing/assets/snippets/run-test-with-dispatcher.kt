package {your.package}.snippets

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Two dispatcher strategies for coroutine testing.
 *
 * UnconfinedTestDispatcher: coroutines run eagerly (good for simple tests).
 * StandardTestDispatcher:   coroutines require explicit advancement (good
 *                           for testing timing-dependent logic like debounce).
 *
 * GOTCHA: runTest auto-advances virtual time for delay() calls. This masks
 * real timing bugs. If the delay duration itself is the thing being tested
 * (e.g., a rate limiter), use StandardTestDispatcher with explicit
 * advanceTimeBy() instead of runTest's auto-advancement.
 */

// --- Strategy 1: Unconfined (eager) ---
@OptIn(ExperimentalCoroutinesApi::class)
class EagerDispatcherExample {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coroutineCompletesImmediately() = runTest(testDispatcher) {
        // Coroutines launched on Main complete immediately.
        // No need to call advanceUntilIdle().
        // Best for: simple state-change tests where you just need
        // the coroutine to finish before asserting.
    }
}

// --- Strategy 2: Standard (explicit advancement) ---
@OptIn(ExperimentalCoroutinesApi::class)
class ExplicitDispatcherExample {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun coroutineRequiresAdvancement() = runTest(testDispatcher) {
        // Launch a coroutine (it will NOT run yet)
        // ...

        // Explicitly advance until all coroutines are idle
        advanceUntilIdle()

        // Now assert on results.
        // Best for: testing that specific delays are respected,
        // debounce behavior, or ordering of emissions over time.
    }
}
