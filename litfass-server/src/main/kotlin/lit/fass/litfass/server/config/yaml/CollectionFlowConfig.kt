package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
data class CollectionFlowConfig @JsonCreator constructor(
    val name: String?,
    val description: String?,
    val applyIf: Map<String, Any> = emptyMap(),
    val steps: List<AbstractCollectionFlowStepConfig>
)
