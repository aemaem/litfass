package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import lit.fass.litfass.server.flow.FlowType

/**
 * @author Michael Mair
 */
class FlowTypeDeserializer : JsonDeserializer<FlowType>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): FlowType {
        return FlowType.valueOf(p!!.text.toUpperCase())
    }
}