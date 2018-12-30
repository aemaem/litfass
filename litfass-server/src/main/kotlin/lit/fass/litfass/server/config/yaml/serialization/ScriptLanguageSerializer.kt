package lit.fass.litfass.server.config.yaml.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import lit.fass.litfass.server.script.ScriptLanguage

/**
 * @author Michael Mair
 */
class ScriptLanguageSerializer : JsonSerializer<ScriptLanguage>() {

    override fun serialize(value: ScriptLanguage?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(value?.name?.toLowerCase())
    }
}