package lit.fass.litfass.server

import lit.fass.litfass.server.ServerApplication
import lit.fass.litfass.server.helper.IntegrationTest
import org.junit.experimental.categories.Category
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import spock.lang.Specification

import static lit.fass.config.Profiles.TEST

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@SpringBootTest(classes = ServerApplication)
@Profile([TEST])
class ServerApplicationSpec extends Specification {

    def "context loads"() {
        expect:
        true
    }
}
