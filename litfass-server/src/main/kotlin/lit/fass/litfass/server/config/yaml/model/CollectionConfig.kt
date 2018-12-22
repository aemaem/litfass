package lit.fass.litfass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lit.fass.litfass.server.config.yaml.serialization.DatastoreDeserializer
import lit.fass.litfass.server.config.yaml.serialization.DatastoreSerializer
import lit.fass.litfass.server.persistence.Datastore

/**
 * @author Michael Mair
 */
data class CollectionConfig @JsonCreator constructor(
    val collection: String,
    val scheduled: String?,
    val retention: String?,
    @JsonSerialize(using = DatastoreSerializer::class)
    @JsonDeserialize(using = DatastoreDeserializer::class)
    val datastore: Datastore = Datastore.POSTGRES,
    @JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
    @JsonSubTypes(value = [JsonSubTypes.Type(value = CollectionFlowConfig::class, name = "flow")])
    val flows: List<CollectionFlowConfig>
)
