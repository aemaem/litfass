package lit.fass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Michael Mair
 */
data class CollectionFlowConfig @JsonCreator constructor(
    val name: String?,
    val description: String?,
    val applyIf: Map<String, Any> = emptyMap(),
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = CollectionFlowStepHttpConfig::class, name = "http"),
            JsonSubTypes.Type(value = CollectionFlowStepScriptConfig::class, name = "script")
        ]
    )
    val steps: List<AbstractCollectionFlowStepConfig>
)
