package lit.fass.server.script

import java.util.Collections.singletonMap

import lit.fass.server.ScriptLanguage.GROOVY
import lit.fass.server.helper.UnitTest
import lit.fass.server.model.Data
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

import scala.jdk.CollectionConverters._

/**
 * @author Michael Mair
 */
@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[UnitTest]))
class GroovyScriptEngineTest extends AnyWordSpec with Matchers {

  val scriptEngine: GroovyScriptEngine = GroovyScriptEngine()

  "groovy script engine" should {

    "be applicable to groovy" in {
      scriptEngine.isApplicable(GROOVY) shouldBe true
    }

    "return expected result" in {
      val script = """[bar: binding["data"]]"""
      val input = singletonMap("foo", Integer.valueOf(1))
      scriptEngine.invoke(script, Data.of(input).toList).get(0) shouldBe Map("bar" -> Map("foo" -> 1).asJava).asJava
    }

  }
}
