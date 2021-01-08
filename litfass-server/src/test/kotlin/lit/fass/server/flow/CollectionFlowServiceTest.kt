package lit.fass.server.flow

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.config.yaml.model.CollectionFlowConfig
import lit.fass.server.config.yaml.model.CollectionFlowStepHttpConfig
import lit.fass.server.config.yaml.model.CollectionFlowStepScriptConfig
import lit.fass.server.flow.FlowAction.ADD
import lit.fass.server.flow.FlowAction.REMOVE
import lit.fass.server.helper.TestTypes.UnitTest
import lit.fass.server.http.HttpService
import lit.fass.server.persistence.Datastore.POSTGRES
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage.KOTLIN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource


/**
 * @author Michael Mair
 */
@Tag(UnitTest)
internal class CollectionFlowServiceTest {

    lateinit var collectionFlowService: CollectionFlowService

    @MockK
    lateinit var httpServiceMock: HttpService

    @MockK(relaxed = true)
    lateinit var scriptEngineMock: ScriptEngine

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { scriptEngineMock.isApplicable(KOTLIN) } returns true
        @Suppress("UNCHECKED_CAST")
        every { scriptEngineMock.invoke("""bindings["data"]""", any()) } answers {
            args[1] as Collection<Map<String, Any?>>
        }

        collectionFlowService = CollectionFlowService(httpServiceMock, listOf(scriptEngineMock))
    }

    @Test
    fun `data is manipulated and returned according to flow config`() {
        val data = mapOf(
            "timestamp" to "0000-01-01T00:00:00Z",
            "foo" to "bar",
            "bar" to true
        )
        val config = CollectionConfig(
            "foo", null, null, POSTGRES, listOf(
                CollectionFlowConfig(
                    null, null, ADD, emptyMap(), listOf(
                        CollectionFlowStepScriptConfig(null, KOTLIN, """bindings["data"]""")
                    )
                )
            )
        )

        val result = collectionFlowService.execute(listOf(data), config).data

        assertThat(result.first()["timestamp"]).isEqualTo("0000-01-01T00:00:00Z")
        assertThat(result.first()["foo"]).isEqualTo("bar")
        assertThat(result.first()["bar"]).isEqualTo(true)
    }

    @Test
    fun `data is manipulated and returned for removal`() {
        val data = mapOf(
            "id" to "1",
            "timestamp" to "0000-01-01T00:00:00Z",
            "foo" to "bar",
            "bar" to true
        )
        val config = CollectionConfig(
            "foo", null, null, POSTGRES, listOf(
                CollectionFlowConfig(
                    null, null, REMOVE, emptyMap(), listOf(
                        CollectionFlowStepScriptConfig(null, KOTLIN, """bindings["data"]""")
                    )
                )
            )
        )

        val result = collectionFlowService.execute(listOf(data), config)

        assertThat(result.action).isEqualTo(REMOVE)
        assertThat(result.data.first()["timestamp"]).isEqualTo("0000-01-01T00:00:00Z")
        assertThat(result.data.first()["foo"]).isEqualTo("bar")
        assertThat(result.data.first()["bar"]).isEqualTo(true)
    }

    @Test
    fun `data is requested with http and returned according to flow config`() {
        val data = mapOf(
            "timestamp" to "0000-01-01T00:00:00Z",
            "foo" to "bar",
            "bar" to true
        )
        val config = CollectionConfig(
            "foo", null, null, POSTGRES, listOf(
                CollectionFlowConfig(
                    null, null, ADD, emptyMap(), listOf(
                        CollectionFlowStepHttpConfig(null, "http://localhost/\${foo}", null, "admin", "admin")
                    )
                )
            )
        )

        every { httpServiceMock.get("http://localhost/bar", any(), "admin", "admin") } returns mapOf("some" to "thing")
        val result = collectionFlowService.execute(listOf(data), config).data

        assertThat(result.first()["timestamp"]).isEqualTo("0000-01-01T00:00:00Z")
        assertThat(result.first()["foo"]).isEqualTo("bar")
        assertThat(result.first()["bar"]).isEqualTo(true)
        assertThat(result.first()["some"]).isEqualTo("thing")
        verify { httpServiceMock.get("http://localhost/bar", any(), "admin", "admin") }
        confirmVerified(httpServiceMock)
    }

    @Test
    fun `list of data is requested with http and script and returned according to flow config`() {
        val data = listOf(
            mapOf("timestamp" to "0000-01-01T00:00:00Z", "foo" to "bar", "bar" to true),
            mapOf("timestamp" to "0000-01-02T00:00:00Z", "foo" to null, "bar" to false)
        )
        val config = CollectionConfig(
            "foo", null, null, POSTGRES, listOf(
                CollectionFlowConfig(
                    null, null, ADD, emptyMap(), listOf(
                        CollectionFlowStepHttpConfig(null, "http://localhost/\${foo}", null, "admin", "admin"),
                        CollectionFlowStepScriptConfig(null, KOTLIN, """bindings["data"]""")
                    )
                )
            )
        )

        every { httpServiceMock.get("http://localhost/bar", any(), "admin", "admin") } returns mapOf("some" to "thing")
        val result = collectionFlowService.execute(data, config).data as List<Map<String, Any?>>

        assertThat(result).hasSize(3)
        assertThat(result.first()["timestamp"]).isEqualTo("0000-01-01T00:00:00Z")
        assertThat(result.first()["foo"]).isEqualTo("bar")
        assertThat(result.first()["bar"]).isEqualTo(true)
        assertThat(result[1]["timestamp"]).isEqualTo("0000-01-02T00:00:00Z")
        assertThat(result[1]["foo"]).isNull()
        assertThat(result[1]["bar"]).isEqualTo(false)
        assertThat(result.last()["some"]).isEqualTo("thing")
        verify { httpServiceMock.get("http://localhost/bar", any(), "admin", "admin") }
        confirmVerified(httpServiceMock)
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        fun `is applicable for applyIf source`() = listOf(
            arguments(emptyMap<String, Any?>(), emptyMap<String, Any?>(), true),
            arguments(emptyMap<String, Any?>(), mapOf("foo" to null), true),
            arguments(mapOf("foo" to null), mapOf("foo" to null), false),
            arguments(mapOf("foo" to true), mapOf("foo" to false), false),
            arguments(mapOf("foo" to true), mapOf("foo" to "false"), false),
            arguments(mapOf("foo" to true), mapOf("foo" to true), true),
            arguments(mapOf("foo" to 1), mapOf("foo" to 1.1), false),
            arguments(mapOf("foo" to 1), mapOf("foo" to 1), true),
            arguments(mapOf("foo" to "bar"), mapOf("foo" to "bar-1"), false),
            arguments(mapOf("foo" to "bar"), mapOf("foo" to "bar"), true)
        )

        @JvmStatic
        fun `variable replacement for data works source`() = listOf(
            //@formatter:off
            arguments("http://localhost/\${foo}"                  , emptyMap<String,Any?>(), "http://localhost/\${foo}"),
            arguments("http://localhost/\${foo}"                  , mapOf("foo" to "bar"), "http://localhost/bar"),
            arguments("http://localhost/\${foo}/\${bar}?s=\${foo}", mapOf("foo" to "bar","bar" to 1), "http://localhost/bar/1?s=bar"),
            arguments("http://localhost/{foo}"                    , mapOf("foo" to "bar"), "http://localhost/{foo}")
            //@formatter:on
        )
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("is applicable for applyIf source")
    fun `is applicable for applyIf`(
        applyIfData: Map<String, Any?>, data: Map<String, Any?>, result: Boolean
    ) {
        assertThat(collectionFlowService.isApplicable(data, applyIfData)).isEqualTo(result)
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("variable replacement for data works source")
    fun `variable replacement for data works`(
        string: String, data: Map<String, Any?>, result: String
    ) {
        assertThat(collectionFlowService.replaceVariables(string, listOf(data))).isEqualTo(result)
    }

}