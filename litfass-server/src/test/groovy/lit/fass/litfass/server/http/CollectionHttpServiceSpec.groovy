package lit.fass.litfass.server.http

import lit.fass.config.ServerConfiguration
import lit.fass.litfass.server.helper.ManualTest
import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * @author Michael Mair
 */
@Category(ManualTest)
class CollectionHttpServiceSpec extends Specification {

    def "test"() {
        expect:
        def result = new CollectionHttpService(new ServerConfiguration().jsonMapper())
                .get("https://circleci.com/api/v1.1/project/github/leftshiftone/keios-protocol/125", [:], "553f9c18552c7314bdfdc7740b7febe05e0ea5a9", "")
        result
    }
}
