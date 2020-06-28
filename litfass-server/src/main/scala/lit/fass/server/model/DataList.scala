package lit.fass.server.model

import java.util

import scala.jdk.CollectionConverters._

/**
 * @author Michael Mair
 */
class DataList extends util.ArrayList[Data] {

}

object DataList {
  def apply(): DataList = new DataList()

  def of(list: util.Collection[Data]): DataList = {
    val dataList = DataList()
    dataList.addAll(list)
    dataList
  }

  def of(list: List[Data]): DataList = {
    val dataList = DataList()
    dataList.addAll(list.asJava)
    dataList
  }

  def of(data: Data): DataList = {
    val dataList = DataList()
    dataList.add(data)
    dataList
  }
}
