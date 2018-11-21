package lit.fass.litfass.server

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(Get, "/").apply {
                assertEquals(OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
