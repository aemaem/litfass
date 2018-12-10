package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME

/**
 * @author Michael Mair
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = CollectionFlowStepHttpConfig::class, name = "http"),
        JsonSubTypes.Type(value = CollectionFlowStepScriptConfig::class, name = "script")
    ]
)
abstract class AbstractCollectionFlowStepConfig @JsonCreator constructor(open val description: String?) {
}