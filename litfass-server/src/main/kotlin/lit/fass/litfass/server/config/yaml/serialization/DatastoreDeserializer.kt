package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import lit.fass.litfass.server.persistence.Datastore

/**
 * @author Michael Mair
 */
class DatastoreDeserializer : JsonDeserializer<Datastore>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Datastore {
        return Datastore.valueOf(p!!.text.toUpperCase())
    }
}