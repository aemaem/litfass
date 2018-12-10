package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME

/**
 * @author Michael Mair
 */
data class CollectionConfig @JsonCreator constructor(
    val collection: String,
    @JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
    @JsonSubTypes(value = [JsonSubTypes.Type(value = CollectionFlowConfig::class, name = "flow")])
    val flows: List<CollectionFlowConfig>
)
