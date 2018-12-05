package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionConfig @JsonCreator constructor(
    val collection: String,
    val flow: List<CollectionComponentConfig>
)
