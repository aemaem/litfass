package lit.fass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import lit.fass.server.flow.FlowAction

/**
 * @author Michael Mair
 */
class FlowActionDeserializer : JsonDeserializer<FlowAction>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): FlowAction {
        return FlowAction.valueOf(p!!.text.toUpperCase())
    }
}