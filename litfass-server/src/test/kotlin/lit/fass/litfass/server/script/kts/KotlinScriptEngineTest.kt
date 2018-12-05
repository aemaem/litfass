package lit.fass.litfass.server.script.kts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Michael Mair
 */
//todo: use spek https://spekframework.org/
class KotlinScriptEngineTest {

    private val kotlinScriptEngine = KotlinScriptEngine()

    @Test
    fun kotlinScript() {
        val script = """mapOf("foo" to bindings["foo"])"""
        val input = mapOf("foo" to 1)
        val result = kotlinScriptEngine.invoke(script, input)
        assertEquals(1, result["foo"])
    }
}