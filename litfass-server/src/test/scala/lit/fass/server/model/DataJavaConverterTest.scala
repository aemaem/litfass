package lit.fass.server.model

import java.{lang, util}

import lit.fass.server.helper.UnitTest
import lit.fass.server.model.DataJavaConverter.{convertListToJava, convertMapToJava}
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

/**
 * @author Michael Mair
 */
@RunWith(classOf[JUnitRunner])
@Category(Array(classOf[UnitTest]))
class DataJavaConverterTest extends AnyWordSpec with Matchers {

  "java conversion" should {

    //todo: add more test cases

    "convert a map" in {
      val input = Map("foo" -> Iterable("foo", "bar"))
      val result = convertMapToJava(input)

      result shouldBe a[util.Map[_, _]]
      result.get("foo") shouldBe a[util.List[_]]
      result.get("foo").asInstanceOf[util.List[String]].get(0) shouldBe a[String]
      result.get("foo").asInstanceOf[util.List[String]].get(1) shouldBe a[String]
    }

    "convert a list" in {
      val input = List(Map("foo" -> Map("foo" -> 1, "bar" -> true)))
      val result = convertListToJava(input)

      result shouldBe a[util.List[_]]
      result.get(0) shouldBe a[util.Map[_, _]]
      result.get(0).get("foo") shouldBe a[util.Map[_, _]]
      result.get(0).get("foo").asInstanceOf[util.Map[String, Object]].get("foo") shouldBe a[lang.Integer]
      result.get(0).get("foo").asInstanceOf[util.Map[String, Object]].get("bar") shouldBe a[lang.Boolean]
    }

  }
}
