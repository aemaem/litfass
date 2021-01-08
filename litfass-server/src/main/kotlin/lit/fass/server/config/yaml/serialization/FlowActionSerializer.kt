package lit.fass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import lit.fass.server.flow.FlowAction

/**
 * @author Michael Mair
 */
class FlowActionSerializer : JsonSerializer<FlowAction>() {

    override fun serialize(value: FlowAction?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.name?.toLowerCase())
    }
}