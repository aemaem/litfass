package lit.fass.server.model

import java.util.Collections
import java.{lang, util}

import lit.fass.server.helper.UnitTest
import lit.fass.server.model.DataScalaConverter.{convertListToScala, convertMapToScala}
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
class DataScalaConverterTest extends AnyWordSpec with Matchers {

  "scala conversion" should {

    //todo: add more test cases

    "convert a map" in {
      val input = Collections.singletonMap("foo", util.Arrays.asList("foo", "bar"))
      val result = convertMapToScala(input)

      result shouldBe a[Map[_, _]]
      result("foo") shouldBe a[List[_]]
      result("foo").asInstanceOf[List[String]](0) shouldBe a[String]
      result("foo").asInstanceOf[List[String]](1) shouldBe a[String]
    }

    "convert a list" in {
      val inputMap = new util.HashMap[String, Object]()
      inputMap.put("foo", Integer.valueOf(1))
      inputMap.put("bar", lang.Boolean.valueOf(true))
      val input = Collections.singletonList(Collections.singletonMap("foo", inputMap))

      val result = convertListToScala(input)

      result shouldBe a[List[_]]

      result(0) shouldBe a[Map[_, _]]
      result(0)("foo") shouldBe a[Map[_, _]]
      result(0)("foo").asInstanceOf[Map[String, Any]]("foo") shouldBe a[Int]
      result(0)("foo").asInstanceOf[Map[String, Any]]("bar") shouldBe a[Boolean]
    }
  }
}
