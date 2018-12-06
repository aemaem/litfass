package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionComponentTransformConfig
import lit.fass.litfass.server.config.yaml.CollectionConfig
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.script.kts.KotlinScriptEngine
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionFlowServiceSpec extends Specification {

    @Subject
    @Shared
    CollectionFlowService collectionFlowService

    def setupSpec() {
        collectionFlowService = new CollectionFlowService()
    }

    def "data is manipulated and returned according to flow config"() {
        given: "some data, a config and script engines"
        def data = [
                timestamp: "0000-01-01T00:00:00Z",
                foo      : "bar",
                bar      : true
        ]
        def config = new CollectionConfig("foo", [
                new CollectionComponentTransformConfig(null, "kts", """bindings["data"]""")
        ])
        def scriptEngines = [new KotlinScriptEngine()]

        when: "flow is executed"
        def result = collectionFlowService.execute(data, config, scriptEngines)

        then: "data is returned"
        result.timestamp == "0000-01-01T00:00:00Z"
        result.foo == "bar"
        result.bar == true
    }
}
