package lit.fass.server.script

import lit.fass.server.ScriptLanguage.ScriptLanguage
import lit.fass.server.model.DataList

/**
 * @author Michael Mair
 */
trait ScriptEngine {
  def isApplicable(language: ScriptLanguage): Boolean

  def invoke(script: String, data: DataList): DataList
}
