package lit.fass.litfass.server.helper

import com.typesafe.config.ConfigFactory
import groovy.json.JsonBuilder
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest

import static io.ktor.http.HttpHeaders.Authorization
import static io.ktor.server.testing.TestApplicationRequestKt.setBody
import static io.ktor.server.testing.TestEngineKt.createTestEnvironment

/**
 * @author Michael Mair
 */
trait KtorSupport {

    TestApplicationEngine initializeApp(Map<String, Object> additionalConfig = [:]) {
        def app = new TestApplicationEngine(createTestEnvironment({
            def appConfig = ConfigFactory.load()
            def customConfig = ConfigFactory.parseMap(additionalConfig)
            it.config = new HoconApplicationConfig(appConfig.withFallback(customConfig).resolve())
        }), {})
        app.start(true)
        return app
    }

    void withBasicAuth(String username, String password, TestApplicationRequest request) {
        request.addHeader(Authorization, "Basic ${Base64.getEncoder().encodeToString(("${username}:${password}".bytes))}")
    }

    void withBody(String body, TestApplicationRequest request) {
        setBody(request, body)
    }

    void withBody(Map<String, Object> body, TestApplicationRequest request) {
        setBody(request, new JsonBuilder(body).toString())
    }
}
