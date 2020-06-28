package lit.fass.server.script

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import lit.fass.server.ScriptLanguage.ScriptLanguage
import lit.fass.server.model.DataList
import lit.fass.server.script.ScriptActor.{ExecuteScriptMessage, ExecutedScriptMessage, ScriptMessage}


object ScriptActor {

  //@formatter:off
  sealed trait ScriptMessage
  final case class ExecuteScriptMessage(language: ScriptLanguage, script: String, data: DataList, replyTo: ActorRef[ExecutedScriptMessage]) extends ScriptMessage
  final case class ExecutedScriptMessage(data: DataList)
  //@formatter:on

  def apply(): ScriptActor =
    Behaviors.setup(context => new ScriptActor(context = context))

  def apply(scriptEngines: List[ScriptEngine]): ScriptActor =
    Behaviors.setup(context => new ScriptActor(scriptEngines, context))
}

/**
 * @author Michael Mair
 */
class ScriptActor(private val scriptEngines: List[ScriptEngine] = List(GroovyScriptEngine()),
                  context: ActorContext[ScriptMessage]) extends AbstractBehavior[ScriptMessage](context) {


  override def onMessage(msg: ScriptMessage): Behavior[ScriptMessage] = {
    msg match {
      case ExecuteScriptMessage(lang, script, data, replyTo) =>
        val engine = scriptEngines.find { engine => engine.isApplicable(lang) }.orNull
        if (engine == null) throw new ScriptException(s"No script engine available for language $language")
        val result = engine.invoke(script, data)
        replyTo ! ExecutedScriptMessage(result)
        this
    }
  }
}
