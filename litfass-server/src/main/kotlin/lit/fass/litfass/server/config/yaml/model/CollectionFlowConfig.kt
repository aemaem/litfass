package lit.fass.litfass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lit.fass.litfass.server.config.yaml.serialization.FlowTypeDeserializer
import lit.fass.litfass.server.config.yaml.serialization.FlowTypeSerializer
import lit.fass.litfass.server.flow.FlowType

/**
 * @author Michael Mair
 */
data class CollectionFlowConfig @JsonCreator constructor(
    val name: String?,
    val description: String?,
    val applyIf: Map<String, Any> = emptyMap(),
    @JsonSerialize(using = FlowTypeSerializer::class)
    @JsonDeserialize(using = FlowTypeDeserializer::class)
    val type: FlowType = FlowType.INSERT_OR_UPDATE,
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = CollectionFlowStepHttpConfig::class, name = "http"),
            JsonSubTypes.Type(value = CollectionFlowStepScriptConfig::class, name = "script")
        ]
    )
    val steps: List<AbstractCollectionFlowStepConfig>
)
