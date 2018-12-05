package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionComponentTransformConfig @JsonCreator constructor(
    override val description: String?,
    val language: String,
    val code: String
) : CollectionComponentConfig(description)
