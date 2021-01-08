package lit.fass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lit.fass.server.config.yaml.serialization.FlowActionDeserializer
import lit.fass.server.config.yaml.serialization.FlowActionSerializer
import lit.fass.server.flow.FlowAction

/**
 * @author Michael Mair
 */
data class CollectionFlowConfig @JsonCreator constructor(
    val name: String?,
    val description: String?,
    @JsonSerialize(using = FlowActionSerializer::class)
    @JsonDeserialize(using = FlowActionDeserializer::class)
    val action: FlowAction = FlowAction.ADD,
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
