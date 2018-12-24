package lit.fass.litfass.server.config.yaml

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.persistence.CollectionConfigPersistenceService
import lit.fass.litfass.server.schedule.SchedulerService
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static java.io.File.createTempFile
import static lit.fass.litfass.server.persistence.Datastore.ELASTICSEARCH
import static lit.fass.litfass.server.persistence.Datastore.POSTGRES

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class YamlConfigServiceSpec extends Specification {

    @Subject
    YamlConfigService yamlConfigService

    CollectionConfigPersistenceService configPersistenceServiceMock
    SchedulerService schedulerServiceMock

    def setup() {
        configPersistenceServiceMock = Mock()
        schedulerServiceMock = Mock()
        yamlConfigService = new YamlConfigService(configPersistenceServiceMock, schedulerServiceMock)
    }

    def "config file can be parsed"() {
        given: "a config file"
        def configFile = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file)

        when: "file is parsed"
        yamlConfigService.readConfig(configFile)
        def result = yamlConfigService.getConfig("foo")

        then: "config is available"
        1 * configPersistenceServiceMock.saveConfig("foo", _)
        0 * configPersistenceServiceMock.findConfig(_) // because it is cached
        1 * schedulerServiceMock.createRetentionJob(_)
        yamlConfigService.getConfigs().size() == 1
        result.collection == "foo"
        result.scheduled == "*/30 * * * * * *"
        result.retention == "P7DT0H0M"
        result.datastore == ELASTICSEARCH
        result.flows.size() == 2
        result.flows[0].name == "Flow 1"
        result.flows[0].description == "Flow description 1"
        result.flows[0].steps.size() == 3
        result.flows[0].steps[0].description == null
        result.flows[0].steps[0].extension == "kts"
        result.flows[0].steps[0].code == """println("foo")"""
        result.flows[0].steps[1].description == null
        result.flows[0].steps[1].url == "https://some.url/foo?bar=true"
        result.flows[0].steps[1].username == "user"
        result.flows[0].steps[1].password == "secret"
        result.flows[0].steps[2].description == null
        result.flows[0].steps[2].extension == "kts"
        result.flows[0].steps[2].code == """println("bar")"""
        result.flows[1].name == null
        result.flows[1].description == null
        result.flows[1].steps.size() == 1
        result.flows[1].steps[0].description == "First step"
        result.flows[1].steps[0].extension == "kts"
        result.flows[1].steps[0].code == """println("foo")"""
    }

    def "config file with multiple configs can be parsed"() {
        given: "a config file"
        def configFile = new File(this.class.getResource("/multiple-configs.yml").file)

        when: "file is parsed"
        yamlConfigService.readConfig(configFile)
        def result1 = yamlConfigService.getConfig("foo1")
        def result2 = yamlConfigService.getConfig("foo2")

        then: "config is available"
        1 * configPersistenceServiceMock.saveConfig("foo1", _)
        1 * configPersistenceServiceMock.saveConfig("foo2", _)
        0 * configPersistenceServiceMock.findConfig(_) // because it is cached
        0 * schedulerServiceMock.createRetentionJob(_)
        yamlConfigService.getConfigs().size() == 2
        result1.collection == "foo1"
        result1.scheduled == null
        result1.retention == null
        result1.datastore == POSTGRES
        result1.flows.size() == 1
        result2.collection == "foo2"
        result2.scheduled == null
        result2.retention == null
        result2.datastore == POSTGRES
        result2.flows.size() == 1
    }

    @Unroll
    def "config file throws exception for invalid collection name '#collectionName'"() {
        given: "a config file with invalid collection name"
        def configFile = createTempFile("config", ".yml")
        configFile.text = """
        collection: $collectionName
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()

        when: "file is parsed"
        yamlConfigService.readConfig(configFile)

        then: "exception is thrown"
        0 * configPersistenceServiceMock._
        0 * schedulerServiceMock._
        thrown(ConfigException)

        where:
        collectionName << ["a", "abc d", "abcö", "a,c", "abcdefghijklmnopqrstuvwxyz0123456789101112113141516"]
    }

    @Unroll
    def "config file throws exception for invalid retention '#retention'"() {
        given: "a config file with invalid collection name"
        def configFile = createTempFile("config", ".yml")
        configFile.text = """
        collection: foo
        retention: "$retention"
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()

        when: "file is parsed"
        yamlConfigService.readConfig(configFile)

        then: "exception is thrown"
        0 * configPersistenceServiceMock._
        0 * schedulerServiceMock._
        thrown(ConfigException)

        where:
        retention << ["1", "P7", "T15M"]
    }

    def "files in directory can be parsed"() {
        given: "a config directory"
        def configDir = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file).parentFile

        when: "directory is read"
        yamlConfigService.readRecursively(configDir)
        def result = yamlConfigService.configs

        then: "configs are available"
        3 * configPersistenceServiceMock.saveConfig(_, _)
        0 * configPersistenceServiceMock.findConfig(_) // because it is cached
        1 * schedulerServiceMock.createRetentionJob(_)
        result.size() == 3
        def fooResult = result.find { it.collection == "foo" }
        fooResult.scheduled == "*/30 * * * * * *"
        fooResult.retention == "P7DT0H0M"
        fooResult.datastore == ELASTICSEARCH
        fooResult.flows.size() == 2
        fooResult.flows[0].name == "Flow 1"
        fooResult.flows[0].description == "Flow description 1"
        fooResult.flows[0].steps.size() == 3
        fooResult.flows[0].steps[0].description == null
        fooResult.flows[0].steps[0].extension == "kts"
        fooResult.flows[0].steps[0].code == """println("foo")"""
        fooResult.flows[0].steps[1].description == null
        fooResult.flows[0].steps[1].url == "https://some.url/foo?bar=true"
        fooResult.flows[0].steps[1].username == "user"
        fooResult.flows[0].steps[1].password == "secret"
        fooResult.flows[0].steps[2].description == null
        fooResult.flows[0].steps[2].extension == "kts"
        fooResult.flows[0].steps[2].code == """println("bar")"""
        fooResult.flows[1].name == null
        fooResult.flows[1].description == null
        fooResult.flows[1].steps.size() == 1
        fooResult.flows[1].steps[0].description == "First step"
        fooResult.flows[1].steps[0].extension == "kts"
        fooResult.flows[1].steps[0].code == """println("foo")"""
        def barResult = result.find { it.collection == "bar" }
        barResult.scheduled == null
        barResult.retention == null
        barResult.datastore == POSTGRES
        barResult.flows.size() == 1
        barResult.flows[0].name == null
        barResult.flows[0].description == null
        barResult.flows[0].steps.size() == 1
        barResult.flows[0].steps[0].description == null
        barResult.flows[0].steps[0].extension == "kts"
        barResult.flows[0].steps[0].code == """println("bar")"""
        def subResult = result.find { it.collection == "sub" }
        subResult.scheduled == null
        subResult.retention == null
        subResult.datastore == POSTGRES
        subResult.flows.size() == 1
        subResult.flows[0].name == null
        subResult.flows[0].description == null
        subResult.flows[0].steps.size() == 2
        subResult.flows[0].steps[0].description == null
        subResult.flows[0].steps[0].url == "https://some.url/foo?bar=true"
        subResult.flows[0].steps[0].username == null
        subResult.flows[0].steps[0].password == null
        subResult.flows[0].steps[1].description == null
        subResult.flows[0].steps[1].extension == "kts"
        subResult.flows[0].steps[1].code == """println("bar")"""
    }

    def "file can be parsed"() {
        given: "a config directory"
        def configDir = new File(this.class.getResource("/config/yaml/fooTestConfig.yml").file)

        when: "file is read recursively"
        yamlConfigService.readRecursively(configDir)
        def result = yamlConfigService.configs

        then: "only the given file is read"
        1 * configPersistenceServiceMock.saveConfig("foo", _)
        0 * configPersistenceServiceMock.findConfig(_) // because it is cached
        1 * schedulerServiceMock.createRetentionJob(_)
        result.size() == 1
        result[0].collection == "foo"
    }

    def "config can be removed"() {
        given: "a config foo"
        yamlConfigService.configCache.put("foo", new CollectionConfig("foo", null, null, POSTGRES, []))

        when: "config foo is removed"
        yamlConfigService.removeConfig("foo")

        then: "config foo is not available in config store"
        1 * configPersistenceServiceMock.deleteConfig("foo")
        0 * configPersistenceServiceMock.saveConfig(_, _)
        0 * configPersistenceServiceMock.findConfig(_) // because it is cached
        1 * schedulerServiceMock.cancelCollectionJob(_)
        1 * schedulerServiceMock.cancelRetentionJob(_)
        yamlConfigService.configCache.size() == 0
    }

    def "configs are read from database"() {
        given: "3 configs in database"
        def config1 = """
        collection: foo1
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        def config2 = """
        collection: foo2
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        def config3 = """
        collection: foo3
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()

        when: "all configs are read"
        yamlConfigService.readConfigsFromDatabase()

        then: "persistence service returns all configs"
        1 * configPersistenceServiceMock.findConfigs() >> [config1, config2, config3]
        yamlConfigService.configCache.size() == 3
    }
}
