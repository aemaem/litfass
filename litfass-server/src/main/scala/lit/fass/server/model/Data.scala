package lit.fass.server.model

import java.util

/**
 * @author Michael Mair
 */
class Data extends util.HashMap[String, Object] {

  def toList: DataList = {
    DataList.of(this)
  }
}

object Data {
  def apply(): Data = new Data()

  def of(map: util.Map[String, _ <: Object]): Data = {
    val data = Data()
    data.putAll(map)
    data
  }

  def of(map: Map[String, _ <: Any]): Data = {
    val data = Data()
    data.putAll(DataJavaConverter.convertMapToJava(map))
    data
  }
}