package {your.package}.snippets

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Common MockEngine patterns for integration tests.
 * See SKILL.md Gotchas for request-ordering and path-matching caveats.
 */

// --- Pattern 1: Simple JSON response ---
val simpleEngine = MockEngine { request ->
    respond(
        content = """{"id": "1", "name": "Test"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}

// --- Pattern 2: Route-based responses ---
val routedEngine = MockEngine { request ->
    when (request.url.encodedPath) {
        "/api/accounts" -> respond(
            content = """[{"id": "1"}, {"id": "2"}]""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        "/api/accounts/1" -> respond(
            content = """{"id": "1", "username": "alice"}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        else -> respondError(HttpStatusCode.NotFound)
    }
}

// --- Pattern 3: Method + path matching ---
val methodAwareEngine = MockEngine { request ->
    val key = "${request.method.value} ${request.url.encodedPath}"
    when (key) {
        "GET /api/accounts" -> respondOk("""[]""")
        "POST /api/accounts" -> respond(
            content = """{"id": "new-1"}""",
            status = HttpStatusCode.Created,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        "DELETE /api/accounts/1" -> respond(
            content = "",
            status = HttpStatusCode.NoContent,
        )
        else -> respondError(HttpStatusCode.NotFound)
    }
}

// --- Pattern 4: Stateful engine (tracks requests) ---
// Use this to assert on WHAT was called, not the ORDER.
fun statefulEngine(): Pair<MockEngine, MutableList<HttpRequestData>> {
    val requestLog = mutableListOf<HttpRequestData>()
    val engine = MockEngine { request ->
        requestLog.add(request)
        respondOk("""{"status": "ok"}""")
    }
    return engine to requestLog
}
// Usage:
// val (engine, log) = statefulEngine()
// ... run test ...
// assertEquals(2, log.size)
// assertTrue(log.any { it.url.encodedPath == "/api/accounts" })

// --- Pattern 5: Sequential responses ---
// Returns a different response on each call. Useful for testing retry logic.
fun sequentialEngine(responses: List<String>): MockEngine {
    var index = 0
    return MockEngine {
        val body = responses.getOrElse(index) { """{"error": "no more responses"}""" }
        index++
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
}

// --- Pattern 6: Error simulation ---
val errorEngine = MockEngine { request ->
    when (request.url.encodedPath) {
        "/api/accounts" -> respond(
            content = """{"error": "Service unavailable"}""",
            status = HttpStatusCode.ServiceUnavailable,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        "/api/auth/login" -> respond(
            content = """{"error": "Invalid credentials"}""",
            status = HttpStatusCode.Unauthorized,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
        else -> respondBadRequest()
    }
}
