package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Michael Mair
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = CollectionComponentHttpConfig::class, name = "http"),
        JsonSubTypes.Type(value = CollectionComponentScriptConfig::class, name = "script")
    ]
)
abstract class CollectionComponentConfig @JsonCreator constructor(open val description: String?) {
}