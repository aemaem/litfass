package lit.fass.litfass.server

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {
    @Test
    fun pathRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(Get, "/").apply {
                assertEquals(OK, response.status())
                assertEquals(
                    """{
  "application" : "LITFASS",
  "description" : "Lightweight Integrated Tailorable Flow Aware Software Service"
}""", response.content
                )
            }
        }
    }

    // todo: test path /collections
}
