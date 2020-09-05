package lit.fass.server.groovy

import lit.fass.server.helper.UnitTest.UnitTest
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.script.groovy.GroovyScriptEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * @author Michael Mair
 */
@Tag(UnitTest)
internal class GroovyScriptEngineTest {

    val scriptEngine = GroovyScriptEngine()

    @ParameterizedTest(name = "{displayName} - {0}")
    @CsvSource(
        "KOTLIN, false",
        "GROOVY, true"
    )
    fun `groovy script engine is applicable`(language: String, expected: Boolean) {
        assertThat(scriptEngine.isApplicable(ScriptLanguage.valueOf(language))).isEqualTo(expected)
    }

    @Test
    fun `groovy script engine returns result`() {
        val script = """[bar: binding["data"]]"""
        val input = mapOf("foo" to 1)
        assertThat(scriptEngine.invoke(script, listOf(input)).first()).isEqualTo(mapOf("bar" to mapOf("foo" to 1)))
    }
}