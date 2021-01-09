package lit.fass.server.yaml

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.server.config.yaml.ConfigException
import lit.fass.server.config.yaml.YamlConfigService
import lit.fass.server.config.yaml.model.CollectionFlowStepHttpConfig
import lit.fass.server.config.yaml.model.CollectionFlowStepScriptConfig
import lit.fass.server.flow.FlowAction
import lit.fass.server.helper.TestTypes.UnitTest
import lit.fass.server.persistence.CollectionConfigPersistenceService
import lit.fass.server.persistence.Datastore.POSTGRES
import lit.fass.server.script.ScriptLanguage.GROOVY
import lit.fass.server.script.ScriptLanguage.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.io.File.createTempFile


/**
 * @author Michael Mair
 */
@Tag(UnitTest)
internal class YamlConfigServiceTest {

    lateinit var yamlConfigService: YamlConfigService

    @MockK(relaxed = true)
    lateinit var configPersistenceServiceMock: CollectionConfigPersistenceService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        yamlConfigService = YamlConfigService(configPersistenceServiceMock)
    }

    @Test
    fun `config file can be parsed`() {
        val configFile = File(this::class.java.getResource("/config/yaml/fooTestConfig.yml").file)
        yamlConfigService.readConfig(configFile)

        every { configPersistenceServiceMock.findConfigs() } returns listOf(configFile.readText())

        assertThat(yamlConfigService.getConfigs()).hasSize(1)
        yamlConfigService.getConfig("foo").also {
            assertThat(it.collection).isEqualTo("foo")
            assertThat(it.scheduled).isEqualTo("*/30 * * * * * *")
            assertThat(it.retention).isEqualTo("P7DT0H0M")
            assertThat(it.datastore).isEqualTo(POSTGRES)
            assertThat(it.flows).hasSize(2)
            it.flows[0].also { flow ->
                assertThat(flow.name).isEqualTo("Flow 1")
                assertThat(flow.description).isEqualTo("Flow description 1")
                assertThat(flow.action).isEqualTo(FlowAction.REMOVE)
                assertThat(flow.steps).hasSize(3)
                (flow.steps[0] as CollectionFlowStepScriptConfig).also { step ->
                    assertThat(step.description).isEqualTo(null)
                    assertThat(step.language).isEqualTo(KOTLIN)
                    assertThat(step.code).isEqualTo("""println("foo")""")
                }
                (flow.steps[1] as CollectionFlowStepHttpConfig).also { step ->
                    assertThat(step.description).isEqualTo(null)
                    assertThat(step.url).isEqualTo("https://some.url/foo?bar=true")
                    assertThat(step.headers).hasSize(1)
                    assertThat(step.headers!![0]["name"]).isEqualTo("foo")
                    assertThat(step.headers!![0]["value"]).isEqualTo("bar")
                    assertThat(step.username).isEqualTo("user")
                    assertThat(step.password).isEqualTo("secret")
                }
                (flow.steps[2] as CollectionFlowStepScriptConfig).also { step ->
                    assertThat(step.description).isEqualTo(null)
                    assertThat(step.language).isEqualTo(KOTLIN)
                    assertThat(step.code).isEqualTo("""println("bar")""")
                }
            }
            it.flows[1].also { flow ->
                assertThat(flow.name).isEqualTo(null)
                assertThat(flow.description).isEqualTo(null)
                assertThat(flow.action).isEqualTo(FlowAction.ADD)
                assertThat(flow.steps).hasSize(1)
                (flow.steps[0] as CollectionFlowStepScriptConfig).also { step ->
                    assertThat(step.description).isEqualTo("First step")
                    assertThat(step.language).isEqualTo(KOTLIN)
                    assertThat(step.code).isEqualTo("""println("foo")""")
                }
            }
        }

        verify(exactly = 1) { configPersistenceServiceMock.saveConfig("foo", any()) }
        verify(exactly = 1) { configPersistenceServiceMock.findConfigs() }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @Test
    fun `config file with multiple configs can be parsed`() {
        val configFile = File(this::class.java.getResource("/multiple-configs.yml").file)
        yamlConfigService.readConfig(configFile)

        every { configPersistenceServiceMock.findConfigs() } returns listOf(configFile.readText())

        assertThat(yamlConfigService.getConfigs()).hasSize(2)
        yamlConfigService.getConfig("foo1").also {
            assertThat(it.collection).isEqualTo("foo1")
            assertThat(it.scheduled).isNull()
            assertThat(it.retention).isNull()
            assertThat(it.datastore).isEqualTo(POSTGRES)
            assertThat(it.flows).hasSize(1)
        }
        yamlConfigService.getConfig("foo2").also {
            assertThat(it.collection).isEqualTo("foo2")
            assertThat(it.scheduled).isNull()
            assertThat(it.retention).isNull()
            assertThat(it.datastore).isEqualTo(POSTGRES)
            assertThat(it.flows).hasSize(1)
        }

        verify(exactly = 1) { configPersistenceServiceMock.saveConfig("foo1", any()) }
        verify(exactly = 1) { configPersistenceServiceMock.saveConfig("foo2", any()) }
        verify(exactly = 1) { configPersistenceServiceMock.findConfigs() }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @CsvSource("a", "abc d", "abcö", "a,c", "abcdefghijklmnopqrstuvwxyz0123456789101112113141516")
    fun `config file throws exception for invalid collection name`(collectionName: String) {
        val configFile = createTempFile("config", ".yml")
        configFile.writeText(
            """
            collection: $collectionName
            flows:
              - flow:
                  steps:
                    - script:
                        language: groovy
                        code: bindings.data
            """.trimIndent()
        )
        assertThatExceptionOfType(ConfigException::class.java).isThrownBy {
            yamlConfigService.readConfig(configFile)
        }.withMessage("Collection name ${collectionName} must match regex ^[a-zA-Z0-9_]{2,50}\$")

        verify(exactly = 0) { configPersistenceServiceMock.saveConfig(any(), any()) }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @CsvSource("1", "P7", "T15M")
    fun `config file throws exception for invalid retention`(retention: String) {
        val configFile = createTempFile("config", ".yml")
        configFile.writeText(
            """
            collection: foo
            retention: "${retention}"
            flows:
              - flow:
                  steps:
                    - script:
                        language: groovy
                        code: bindings.data
            """.trimIndent()
        )
        assertThatExceptionOfType(ConfigException::class.java).isThrownBy {
            yamlConfigService.readConfig(configFile)
        }.withMessage("Retention duration ${retention} of collection name foo is not a valid ISO-8601 format")

        verify(exactly = 0) { configPersistenceServiceMock.saveConfig(any(), any()) }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @Test
    fun `files in directory can be parsed`() {
        val configDir = File(this::class.java.getResource("/config/yaml/fooTestConfig.yml").file).parentFile
        yamlConfigService.readRecursively(configDir)

        every { configPersistenceServiceMock.findConfigs() } returns configDir.walkTopDown()
            .filter { it.isFile && (it.name.endsWith("yml") || it.name.endsWith("yaml")) }
            .map { it.readText() }
            .toList()

        yamlConfigService.getConfigs().also {
            assertThat(it).hasSize(3)
            it.find { config -> config.collection == "foo" }.also { fooResult ->
                assertThat(fooResult!!).isNotNull
                assertThat(fooResult.scheduled).isEqualTo("*/30 * * * * * *")
                assertThat(fooResult.retention).isEqualTo("P7DT0H0M")
                assertThat(fooResult.datastore).isEqualTo(POSTGRES)
                assertThat(fooResult.flows).hasSize(2)
                fooResult.flows[0].also { flow ->
                    assertThat(flow.name).isEqualTo("Flow 1")
                    assertThat(flow.description).isEqualTo("Flow description 1")
                    assertThat(flow.steps).hasSize(3)
                    (flow.steps[0] as CollectionFlowStepScriptConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.language).isEqualTo(KOTLIN)
                        assertThat(step.code).isEqualTo("""println("foo")""")
                    }
                    (flow.steps[1] as CollectionFlowStepHttpConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.url).isEqualTo("https://some.url/foo?bar=true")
                        assertThat(step.username).isEqualTo("user")
                        assertThat(step.password).isEqualTo("secret")
                    }
                    (flow.steps[2] as CollectionFlowStepScriptConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.language).isEqualTo(KOTLIN)
                        assertThat(step.code).isEqualTo("""println("bar")""")
                    }
                }
                fooResult.flows[1].also { flow ->
                    assertThat(flow.name).isEqualTo(null)
                    assertThat(flow.description).isEqualTo(null)
                    assertThat(flow.steps).hasSize(1)
                    (flow.steps[0] as CollectionFlowStepScriptConfig).also { step ->
                        assertThat(step.description).isEqualTo("First step")
                        assertThat(step.language).isEqualTo(KOTLIN)
                        assertThat(step.code).isEqualTo("""println("foo")""")
                    }
                }
            }
            it.find { config -> config.collection == "bar" }.also { barResult ->
                assertThat(barResult!!).isNotNull
                assertThat(barResult.scheduled).isEqualTo(null)
                assertThat(barResult.retention).isEqualTo(null)
                assertThat(barResult.datastore).isEqualTo(POSTGRES)
                assertThat(barResult.flows).hasSize(1)
                barResult.flows[0].also { flow ->
                    assertThat(flow.name).isEqualTo(null)
                    assertThat(flow.description).isEqualTo(null)
                    assertThat(flow.steps).hasSize(1)
                    (flow.steps[0] as CollectionFlowStepScriptConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.language).isEqualTo(GROOVY)
                        assertThat(step.code).isEqualTo("""println("bar")""")
                    }
                }
            }
            it.find { config -> config.collection == "sub" }.also { subResult ->
                assertThat(subResult!!).isNotNull
                assertThat(subResult.scheduled).isEqualTo(null)
                assertThat(subResult.retention).isEqualTo(null)
                assertThat(subResult.datastore).isEqualTo(POSTGRES)
                assertThat(subResult.flows).hasSize(1)
                subResult.flows[0].also { flow ->
                    assertThat(flow.name).isEqualTo(null)
                    assertThat(flow.description).isEqualTo(null)
                    assertThat(flow.steps).hasSize(2)
                    (flow.steps[0] as CollectionFlowStepHttpConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.url).isEqualTo("https://some.url/foo?bar=true")
                        assertThat(step.username).isEqualTo(null)
                        assertThat(step.password).isEqualTo(null)
                    }
                    (flow.steps[1] as CollectionFlowStepScriptConfig).also { step ->
                        assertThat(step.description).isEqualTo(null)
                        assertThat(step.language).isEqualTo(KOTLIN)
                        assertThat(step.code).isEqualTo("""println("bar")""")
                    }
                }
            }
        }

        verify(exactly = 3) { configPersistenceServiceMock.saveConfig(any(), any()) }
        verify(exactly = 1) { configPersistenceServiceMock.findConfigs() }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @Test
    fun `file can be parsed`() {
        val configFile = File(this::class.java.getResource("/config/yaml/fooTestConfig.yml").file)

        every { configPersistenceServiceMock.findConfigs() } returns listOf(configFile.readText())

        yamlConfigService.readRecursively(configFile)

        yamlConfigService.getConfigs().also {
            assertThat(it).hasSize(1)
            assertThat(it.first().collection).isEqualTo("foo")
        }

        verify(exactly = 1) { configPersistenceServiceMock.saveConfig(any(), any()) }
        verify(exactly = 1) { configPersistenceServiceMock.findConfigs() }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached
        confirmVerified(configPersistenceServiceMock)
    }

    @Test
    fun `config can be removed`() {
        val configFile = File(this::class.java.getResource("/config/yaml/fooTestConfig.yml").file)
        yamlConfigService.readRecursively(configFile)

        yamlConfigService.removeConfig("foo")

        verify(exactly = 1) { configPersistenceServiceMock.saveConfig(any(), any()) }
        verify(exactly = 0) { configPersistenceServiceMock.findConfig(any()) } // because it is cached

        verify(exactly = 1) { configPersistenceServiceMock.deleteConfig("foo") }
        confirmVerified(configPersistenceServiceMock)

        assertThat(yamlConfigService.getConfigs()).isEmpty()
    }

    @Test
    fun `configs are read from database`() {
        val config1 = """
        collection: foo1
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kotlin
                    code: bindings["data"]
        """.trimIndent()
        val config2 = """
        collection: foo2
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kotlin
                    code: bindings["data"]
        """.trimIndent()
        val config3 = """
        collection: foo3
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    language: kotlin
                    code: bindings["data"]
        """.trimIndent()
        every { configPersistenceServiceMock.findConfigs() } returns listOf(config1, config2, config3)

        yamlConfigService.readConfigsFromDatabase()

        assertThat(yamlConfigService.getConfigs()).hasSize(3)
    }

}