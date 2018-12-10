package lit.fass.litfass.server.flow

import lit.fass.litfass.server.config.yaml.CollectionConfig
import lit.fass.litfass.server.config.yaml.CollectionFlowConfig
import lit.fass.litfass.server.config.yaml.CollectionFlowStepHttpConfig
import lit.fass.litfass.server.config.yaml.CollectionFlowStepScriptConfig
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.http.HttpService
import lit.fass.litfass.server.script.ScriptEngine
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionFlowServiceSpec extends Specification {

    @Subject
    CollectionFlowService collectionFlowService

    HttpService httpServiceMock
    ScriptEngine scriptEngineMock

    def setup() {
        httpServiceMock = Mock()
        scriptEngineMock = Mock()
        scriptEngineMock.isApplicable("kts") >> true
        scriptEngineMock.invoke("""bindings["data"]""", _ as Map) >> { args -> return args[1] }
        collectionFlowService = new CollectionFlowService(httpServiceMock, [scriptEngineMock])
    }

    def "data is manipulated and returned according to flow config"() {
        given: "some data and a config"
        def data = [
                timestamp: "0000-01-01T00:00:00Z",
                foo      : "bar",
                bar      : true
        ]
        def config = new CollectionConfig("foo", [new CollectionFlowConfig(null, null, [:], [
                new CollectionFlowStepScriptConfig(null, "kts", """bindings["data"]""")
        ])])

        when: "flow is executed"
        def result = collectionFlowService.execute(data, config)

        then: "data is returned"
        result.timestamp == "0000-01-01T00:00:00Z"
        result.foo == "bar"
        result.bar == true
        0 * httpServiceMock._
    }

    def "data is requested with http and returned according to flow config"() {
        given: "some data and a config"
        def data = [
                timestamp: "0000-01-01T00:00:00Z",
                foo      : "bar",
                bar      : true
        ]
        def config = new CollectionConfig("foo", [new CollectionFlowConfig(null, null, [:], [
                new CollectionFlowStepHttpConfig(null, "http://localhost/\${foo}", "admin", "admin")
        ])])

        when: "flow is executed"
        def result = collectionFlowService.execute(data, config)

        then: "data is returned"
        1 * httpServiceMock.get("http://localhost/bar", "admin", "admin") >> [some: "thing"]
        result.timestamp == "0000-01-01T00:00:00Z"
        result.foo == "bar"
        result.bar == true
        result.some == "thing"
    }

    @Unroll
    def "#applyIfData is applicable for #data: #expected"() {
        expect:
        collectionFlowService.isApplicable(data, applyIfData) == expected

        where:
        applyIfData  | data           || expected
        [:]          | [:]            || true
        [:]          | [foo: null]    || true
        [foo: null]  | [foo: null]    || false
        [foo: true]  | [foo: false]   || false
        [foo: true]  | [foo: "false"] || false
        [foo: true]  | [foo: true]    || true
        [foo: 1]     | [foo: 1.1]     || false
        [foo: 1]     | [foo: 1]       || true
        [foo: "bar"] | [foo: "bar-1"] || false
        [foo: "bar"] | [foo: "bar"]   || true
    }

    @Unroll
    def "variable replacement for data #data results in #expected"() {
        expect:
        collectionFlowService.replaceVariables(string, data) == expected

        where:
        string                                       | data                 || expected
        "http://localhost/\${foo}"                   | [:]                  || "http://localhost/\${foo}"
        "http://localhost/\${foo}"                   | [foo: "bar"]         || "http://localhost/bar"
        "http://localhost/\${foo}/\${bar}?s=\${foo}" | [foo: "bar", bar: 1] || "http://localhost/bar/1?s=bar"
        "http://localhost/{foo}"                     | [foo: "bar"]         || "http://localhost/{foo}"
    }
}
