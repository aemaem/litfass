package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.helper.UnitTest
import org.junit.experimental.categories.Category
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static lit.fass.litfass.server.script.ScriptLanguage.GROOVY
import static lit.fass.litfass.server.script.ScriptLanguage.KOTLIN

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class KotlinScriptEngineSpec extends Specification {

    @Subject
    @Shared
    KotlinScriptEngine kotlinScriptEngine

    def setupSpec() {
        kotlinScriptEngine = new KotlinScriptEngine()
    }

    @Unroll
    def "kotlin script engine is applicable for #language: #expected"() {
        when: "script engine is requested for #language"
        def result = kotlinScriptEngine.isApplicable(language)

        then: "it is #expected"
        result == expected

        where:
        language || expected
        KOTLIN   || true
        GROOVY   || false
    }

    def "kotlin script engine returns result"() {
        given: "a script and an input"
        def script = """mapOf("bar" to bindings["data"])"""
        def input = [foo: 1]

        when: "the script is invoked"
        def result = kotlinScriptEngine.invoke(script, [input])

        then: "result is returned"
        result.first() == [bar: [foo: 1]]
    }
}
