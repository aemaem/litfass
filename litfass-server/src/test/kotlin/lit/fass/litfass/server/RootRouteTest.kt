package lit.fass.litfass.server

import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Michael Mair
 */
//todo: use spek https://spekframework.org/
class RootRouteTest {

    @Test
    fun pathRoot() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                put("litfass.elasticsearch.client.urls", "http://localhost:9200")
            }
            module(testing = true)
        }) {
            handleRequest(Get, "/") {
                addHeader(Authorization, "Basic ${Base64.getEncoder().encodeToString(("admin:admin".toByteArray()))}")
            }.apply {
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

    //todo: negative test for authentication
}
