package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

data class CollectionConfig @JsonCreator constructor(
    val collection: String,
    val script: CollectionScriptConfig
)
