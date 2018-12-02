package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import lit.fass.litfass.server.config.ConfigService
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Michael Mair
 */
class YamlConfigService : ConfigService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    private val configStore = ConcurrentHashMap<String, CollectionConfig>()
    private val mapper = ObjectMapper(YAMLFactory()).apply { registerModule(KotlinModule()) }

    override fun readConfig(file: File) {
        readConfig(file.name, file.inputStream())
    }

    override fun readConfig(name: String, inputStream: InputStream) {
        log.debug("Reading config $name")
        val config = mapper.readValue(inputStream, CollectionConfig::class.java)
        configStore[name] = config
    }

    override fun getConfig(name: String): CollectionConfig {
        return configStore[name] ?: throw ConfigException("Config with name $name not found")
    }
}