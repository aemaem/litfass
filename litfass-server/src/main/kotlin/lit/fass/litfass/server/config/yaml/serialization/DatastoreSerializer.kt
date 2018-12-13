package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import lit.fass.litfass.server.persistence.Datastore

/**
 * @author Michael Mair
 */
class DatastoreSerializer : JsonSerializer<Datastore>() {

    override fun serialize(value: Datastore?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.name?.toLowerCase())
    }
}