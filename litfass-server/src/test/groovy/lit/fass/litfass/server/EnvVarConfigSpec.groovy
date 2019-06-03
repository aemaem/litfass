package lit.fass.litfass.server


import io.ktor.server.testing.TestApplicationEngine
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.KtorSupport
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class EnvVarConfigSpec extends Specification implements KtorSupport {

    @Shared
    TestApplicationEngine app

    def setupSpec() {
        app = initializeApp([testing: true])
    }

    def cleanupSpec() {
        stopApp(app)
    }

    def "env vars are available"() {
        expect:
        app.environment.config.configList("litfass.config.security.users").collectEntries {
            [it.property("name").string, it.property("password").string]
        } == [foo: "foo", bar: "bar"]
    }
}
