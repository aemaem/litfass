package lit.fass.server.model

import java.util
import java.util.Collections.{emptyList, emptyMap}

import scala.collection.{Map, Seq}


/**
 * @author Michael Mair
 */
object DataJavaConverter {

  def convertListToJava(list: Seq[Map[String, Any]]): util.List[util.Map[String, Object]] = {
    if (list == null || list.isEmpty) return emptyList()

    val converted: util.List[util.Map[String, Object]] = new util.ArrayList(list.size)
    list.foreach(it => converted.add(convertMapToJava(it)))
    converted
  }

  def convertMapToJava(map: Map[String, Any]): util.Map[String, Object] = {
    if (map == null || map.isEmpty) return emptyMap()

    val converted: util.Map[String, Object] = new util.HashMap(map.size)
    map.foreachEntry((key, value) => converted.put(key, convert(value)))
    converted
  }

  private def convert(value: Any): Object = {
    if (value == null) return null

    value match {
      case map: Map[_, _] =>
        val converted = new util.HashMap[Object, Object](map.size)
        map.foreachEntry((key, value) => converted.put(convert(key), convert(value)))
        converted
      case sequence: Seq[_] =>
        val converted = new util.ArrayList[Object](sequence.size)
        sequence.foreach(it => converted.add(convert(it)))
        converted
      case any: Double => double2Double(any).asScala
      case any: Float => float2Float(any)
      case any: Long => long2Long(any)
      case any: Int => int2Integer(any)
      case any: Short => short2Short(any)
      case any: Byte => byte2Byte(any)
      case any: Boolean => boolean2Boolean(any)
      case any: Char => char2Character(any)
      case _ => value.asInstanceOf[Object]
    }
  }
}
