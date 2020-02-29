package lit.fass.litfass.server

import lit.fass.litfass.server.ServerApplication
import lit.fass.litfass.server.helper.IntegrationTest
import org.junit.experimental.categories.Category
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import static lit.fass.config.Profiles.POSTGRES
import static lit.fass.config.Profiles.TEST

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
@SpringBootTest(classes = ServerApplication)
@ActiveProfiles([TEST, POSTGRES])
class ServerApplicationSpec extends Specification {

    def "context loads"() {
        expect:
        true
    }
}
