package lit.fass.litfass.server.script.groovy

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
class GroovyScriptEngineSpec extends Specification {

    @Subject
    @Shared
    GroovyScriptEngine groovyScriptEngine

    def setupSpec() {
        groovyScriptEngine = new GroovyScriptEngine()
    }

    @Unroll
    def "groovy script engine is applicable for #language: #expected"() {
        when: "script engine is requested for #language"
        def result = groovyScriptEngine.isApplicable(language)

        then: "it is #expected"
        result == expected

        where:
        language || expected
        KOTLIN   || false
        GROOVY   || true
    }

    def "groovy script engine returns result"() {
        given: "a script and an input"
        def script = """[bar: binding["data"]]"""
        def input = [foo: 1]

        when: "the script is invoked"
        def result = groovyScriptEngine.invoke(script, [input])

        then: "result is returned"
        result.first() == [bar: [foo: 1]]
    }
}
