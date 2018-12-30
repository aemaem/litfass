package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import lit.fass.litfass.server.script.ScriptLanguage

/**
 * @author Michael Mair
 */
class ScriptLanguageDeserializer : JsonDeserializer<ScriptLanguage>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ScriptLanguage {
        return ScriptLanguage.valueOf(p!!.text.toUpperCase())
    }
}