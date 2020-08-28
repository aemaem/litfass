package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import lit.fass.litfass.server.flow.FlowType

/**
 * @author Michael Mair
 */
class FlowTypeSerializer : JsonSerializer<FlowType>() {

    override fun serialize(value: FlowType?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.name?.toLowerCase())
    }
}