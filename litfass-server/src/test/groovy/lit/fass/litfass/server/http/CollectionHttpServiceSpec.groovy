package lit.fass.litfass.server.http

import lit.fass.config.ServerConfiguration
import lit.fass.litfass.server.helper.UnitTest
import org.junit.experimental.categories.Category
import spock.lang.Specification

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionHttpServiceSpec extends Specification {

    def "test"() {
        expect:
        def result = new CollectionHttpService(new ServerConfiguration().jsonMapper())
                .get("https://circleci.com/api/v1.1/project/github/leftshiftone/keios-protocol/125", [:], "553f9c18552c7314bdfdc7740b7febe05e0ea5a9", "")
        result
    }

    def "test with headers"() {
        expect:
        def result = new CollectionHttpService(new ServerConfiguration().jsonMapper())
                .get("https://circleci.com/api/v1.1/project/github/leftshiftone/keios-protocol/125", ["X-foo": "bar"], "553f9c18552c7314bdfdc7740b7febe05e0ea5a9", "")
        result["requestHeaders"] == ["X-foo": "bar"]
    }

    def "response can be parsed for map"() {
        given:
        def response = '{"foo": "bar","email": "foo@bar"}'.bytes
        when:
        def result = new CollectionHttpService(new ServerConfiguration().jsonMapper()).parse(response)
        then:
        result.foo == "bar"
        result.email == "foo@bar"
    }

    def "response can be parsed for list"() {
        given:
        def response = '[{"foo": "bar","email": "foo@bar"},{"bar": "foo","email": "bar@foo"}]'.bytes
        when:
        def result = new CollectionHttpService(new ServerConfiguration().jsonMapper()).parse(response)
        then:
        result.http[0].foo == "bar"
        result.http[0].email == "foo@bar"
        result.http[1].bar == "foo"
        result.http[1].email == "bar@foo"
    }
}
