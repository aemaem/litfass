package lit.fass.litfass.server.config.yaml

import lit.fass.litfass.server.helper.UnitTest
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class YamlConfigServiceSpec extends Specification {

    @Subject
    YamlConfigService yamlConfigService

    def setup() {
        yamlConfigService = new YamlConfigService()
    }

    def "config file can be parsed"() {
        given: "a config file"
        def configFile = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file)

        when: "file is parsed"
        yamlConfigService.readConfig(configFile)
        def result = yamlConfigService.getConfig("foo")

        then: "config is available"
        yamlConfigService.getConfigs().size() == 1
        result.collection == "foo"
        result.flow.size() == 3
        result.flow[0].description == "First step"
        result.flow[0].language == "kts"
        result.flow[0].code == """println("foo")"""
        result.flow[1].description == "Second step"
        result.flow[1].url == "https://some.url/foo?bar=true"
        result.flow[1].username == "user"
        result.flow[1].password == "secret"
        result.flow[2].description == null
        result.flow[2].language == "kts"
        result.flow[2].code == """println("bar")"""
    }

    def "files in directory can be parsed"() {
        given: "a config directory"
        def configDir = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file).parentFile

        when: "directory is read"
        yamlConfigService.readRecursively(configDir)
        def result = yamlConfigService.configs

        then: "configs are available"
        result.size() == 3
        def fooResult = result.find { it.collection == "foo" }
        fooResult.flow.size() == 3
        fooResult.flow[0].description == "First step"
        fooResult.flow[0].language == "kts"
        fooResult.flow[0].code == """println("foo")"""
        fooResult.flow[1].description == "Second step"
        fooResult.flow[1].url == "https://some.url/foo?bar=true"
        fooResult.flow[1].username == "user"
        fooResult.flow[1].password == "secret"
        fooResult.flow[2].description == null
        fooResult.flow[2].language == "kts"
        fooResult.flow[2].code == """println("bar")"""
        def barResult = result.find { it.collection == "bar" }
        barResult.flow.size() == 1
        barResult.flow[0].description == null
        barResult.flow[0].language == "kts"
        barResult.flow[0].code == """println("bar")"""
        def subResult = result.find { it.collection == "sub" }
        subResult.flow.size() == 2
        subResult.flow[0].description == null
        subResult.flow[0].url == "https://some.url/foo?bar=true"
        subResult.flow[0].username == null
        subResult.flow[0].password == null
        subResult.flow[1].description == null
        subResult.flow[1].language == "kts"
        subResult.flow[1].code == """println("bar")"""
    }

    def "file can be parsed"() {
        given: "a config directory"
        def configDir = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file)

        when: "file is read recursively"
        yamlConfigService.readRecursively(configDir)
        def result = yamlConfigService.configs

        then: "only the given file is read"
        result.size() == 1
        result[0].collection == "foo"
        result[0].flow.size() == 3
        result[0].flow[0].description == "First step"
        result[0].flow[0].language == "kts"
        result[0].flow[0].code == """println("foo")"""
        result[0].flow[1].description == "Second step"
        result[0].flow[1].url == "https://some.url/foo?bar=true"
        result[0].flow[1].username == "user"
        result[0].flow[1].password == "secret"
        result[0].flow[2].description == null
        result[0].flow[2].language == "kts"
        result[0].flow[2].code == """println("bar")"""
    }

    def "config can be removed"() {
        given: "a config foo"
        yamlConfigService.configStore.put("foo", new CollectionConfig("foo", []))

        when: "config foo is removed"
        yamlConfigService.removeConfig("foo")

        then: "config foo is not available in config store"
        yamlConfigService.configStore.isEmpty()
    }
}
