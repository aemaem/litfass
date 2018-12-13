package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Michael Mair
 */
class YamlConfigService : ConfigService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val collectionNameRegex = Regex("^[a-zA-Z0-9-_]{2,30}$")
    }

    private val configStore = ConcurrentHashMap<String, CollectionConfig>()
    private val yamlMapper = ObjectMapper(YAMLFactory()).apply { registerModule(KotlinModule()) }

    override fun readRecursively(file: File) {
        log.debug("Reading recursively ${file.absolutePath}")
        if (!file.exists()) {
            log.warn("Path ${file.absolutePath} does not exist. No configs read.")
            return
        }
        if (file.isFile) {
            readConfig(file)
            return
        }
        file.walkTopDown()
            .filter { it.isFile && (it.name.endsWith("yml") || it.name.endsWith("yaml")) }
            .forEach { readConfig(it) }
    }

    override fun readConfig(file: File): CollectionConfig {
        log.debug("Reading config from file ${file.absolutePath}")
        return readConfig(file.inputStream())
    }

    override fun readConfig(inputStream: InputStream): CollectionConfig {
        val config = yamlMapper.readValue(inputStream, CollectionConfig::class.java)
        if (!collectionNameRegex.matches(config.collection)) {
            throw ConfigException("Collection name ${config.collection} must match regex ${collectionNameRegex.pattern}")
        }

        log.debug("Adding config ${config.collection}")
        if (config == null) {
            throw ConfigException("Config must not be null")
        }
        configStore[config.collection] = config
        return config
    }

    override fun getConfig(name: String): CollectionConfig {
        return configStore[name] ?: throw ConfigException("Config with name $name not found")
    }

    override fun getConfigs(): Collection<CollectionConfig> {
        return configStore.values
    }

    override fun removeConfig(name: String) {
        configStore.remove(name)
    }
}