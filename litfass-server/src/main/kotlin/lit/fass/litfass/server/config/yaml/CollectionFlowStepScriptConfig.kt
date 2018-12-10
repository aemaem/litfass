package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionFlowStepScriptConfig @JsonCreator constructor(
    override val description: String?,
    val extension: String,
    val code: String
) : AbstractCollectionFlowStepConfig(description)
