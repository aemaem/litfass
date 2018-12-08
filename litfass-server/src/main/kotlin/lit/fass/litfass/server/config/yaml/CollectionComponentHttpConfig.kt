package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionComponentHttpConfig @JsonCreator constructor(
    override val description: String?,
    val url: String,
    val username: String?,
    val password: String?
) : CollectionComponentConfig(description)