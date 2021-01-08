package lit.fass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionFlowStepHttpConfig @JsonCreator constructor(
    override val description: String?,
    val url: String,
    val headers: List<Map<String, String?>>?,
    val username: String?,
    val password: String?
) : AbstractCollectionFlowStepConfig(description)