package lit.fass.server.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Behaviors.same
import akka.actor.typed.javadsl.Receive
import lit.fass.server.actor.ScriptActor.Message
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptException
import lit.fass.server.script.ScriptLanguage
import java.util.*


/**
 * @author Michael Mair
 */
class ScriptActor private constructor(
    private val scriptEngines: List<ScriptEngine>,
    context: ActorContext<Message>?
) : AbstractBehavior<Message>(context) {

    companion object {
        @JvmStatic
        fun create(scriptEngines: List<ScriptEngine>): Behavior<Message> {
            return Behaviors.setup { context -> ScriptActor(scriptEngines, context) }
        }
    }

    interface Message : SerializationMarker
    data class TestScript(val language: ScriptLanguage, val payload: Map<*, *>, val replyTo: ActorRef<ScriptResult>) : Message
    data class ScriptResult(val result: Collection<Map<String, Any?>>) : Message

    override fun createReceive(): Receive<Message> = newReceiveBuilder()
        .onMessage(TestScript::class.java) {
            val scriptEngine = scriptEngines.find { engine -> engine.isApplicable(it.language) }
                ?: throw ScriptException("No script engine available for language ${it.language}")

            val script = it.payload["script"] as String

            @Suppress("UNCHECKED_CAST")
            val data = it.payload["data"] as Map<String, Any?>

            val result = scriptEngine.invoke(script, Collections.singletonList(data))
            it.replyTo.tell(ScriptResult(result))
            same()
        }
        .build()

}