package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.cache.CacheBuilder
import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.persistence.CollectionConfigPersistenceService
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable

/**
 * @author Michael Mair
 */
class YamlConfigService(private val configPersistenceService: CollectionConfigPersistenceService) : ConfigService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val collectionNameRegex = Regex("^[a-zA-Z0-9_]{2,50}$")
    }

    private val configCache = CacheBuilder.newBuilder()
        .maximumSize(1024)
        .build<String, CollectionConfig>()
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

        log.info("Adding config ${config.collection}")
        if (config == null) {
            throw ConfigException("Config must not be null")
        }
        try {
            configPersistenceService.saveConfig(config.collection, yamlMapper.writeValueAsString(config))
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ConfigException("Unable to save config ${config.collection} in database")
        }
        configCache.put(config.collection, config)
        return config
    }

    override fun getConfig(name: String): CollectionConfig {
        return loadConfig(name) ?: throw ConfigException("Config with name $name not found")
    }

    override fun getConfigs(): Collection<CollectionConfig> {
        return configCache.asMap().values
    }

    override fun removeConfig(name: String) {
        log.info("Removing config $name")
        try {
            configPersistenceService.deleteConfig(name)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ConfigException("Unable to delete config $name in database")
        }
        configCache.invalidate(name)
    }

    private fun loadConfig(name: String): CollectionConfig? {
        return configCache.get(name, Callable {
            val configData = configPersistenceService.findConfig(name) ?: return@Callable null
            return@Callable yamlMapper.readValue(configData, CollectionConfig::class.java)
        })
    }
}