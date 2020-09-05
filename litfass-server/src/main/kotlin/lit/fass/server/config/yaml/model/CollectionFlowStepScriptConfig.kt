package lit.fass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import lit.fass.server.config.yaml.serialization.ScriptLanguageDeserializer
import lit.fass.server.config.yaml.serialization.ScriptLanguageSerializer
import lit.fass.server.script.ScriptLanguage

/**
 * @author Michael Mair
 */
data class CollectionFlowStepScriptConfig @JsonCreator constructor(
    override val description: String?,
    @JsonSerialize(using = ScriptLanguageSerializer::class)
    @JsonDeserialize(using = ScriptLanguageDeserializer::class)
    val language: ScriptLanguage = ScriptLanguage.GROOVY,
    val code: String
) : AbstractCollectionFlowStepConfig(description)
