package lit.fass.server.model

import java.{lang, util}

import scala.collection.{Map, Seq}

/**
 * @author Michael Mair
 */
object DataScalaConverter {

  def convertListToScala(list: util.List[_ <: util.Map[String, _ <: Object]]): Seq[Map[String, Any]] = {
    if (list == null || list.isEmpty) return Seq.empty

    var converted: Seq[Map[String, Any]] = List()
    list.forEach(it => converted = converted.concat(List(convertMapToScala(it))))
    converted
  }

  def convertMapToScala(map: util.Map[String, _ <: Object]): Map[String, Any] = {
    if (map == null || map.isEmpty) return Map.empty

    var converted: Map[String, Any] = Map()
    map.forEach((key, value) => converted = converted.concat(Map(key -> convert(value))))
    converted
  }

  private def convert(value: Object): Any = {
    if (value == null) return null

    value match {
      case map: util.Map[Object, Object] =>
        var converted: Map[Any, Any] = Map()
        map.forEach((key, value) => converted = converted.concat(Map(convert(key) -> convert(value))))
        converted
      case collection: util.Collection[Object] =>
        var converted: List[Any] = List()
        collection.forEach(it => converted = converted.concat(List(convert(it))))
        converted
      case obj: lang.Double => Double2double(obj)
      case obj: lang.Float => Float2float(obj)
      case obj: lang.Long => Long2long(obj)
      case obj: lang.Integer => Integer2int(obj)
      case obj: lang.Short => Short2short(obj)
      case obj: lang.Byte => Byte2byte(obj)
      case obj: lang.Boolean => Boolean2boolean(obj)
      case obj: lang.Character => Character2char(obj)
      case _ => value.asInstanceOf[Any]
    }
  }
}
