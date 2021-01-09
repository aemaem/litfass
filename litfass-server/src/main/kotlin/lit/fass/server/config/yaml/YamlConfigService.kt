package lit.fass.server.config.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import lit.fass.server.config.ConfigService
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.persistence.CollectionConfigPersistenceService
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.util.function.Function

/**
 * @author Michael Mair
 */
class YamlConfigService(
    private val configPersistenceService: CollectionConfigPersistenceService,
    private val properties: Config = ConfigFactory.defaultApplication()
) : ConfigService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val collectionNameRegex = Regex("^[a-zA-Z0-9_]{2,50}$")
    }

    private val configCache = Caffeine.newBuilder()
        .maximumSize(1024)
        .build<String, CollectionConfig>()
    private val yamlMapper =
        ObjectMapper(YAMLFactory()).registerKotlinModule()

    override fun initializeConfigs() {
        log.info("Reading collection configs from database.")
        readConfigsFromDatabase()

        val collectionPath = properties.getString("litfass.config.collection-path")
        if (collectionPath.isBlank()) {
            log.info("Collection path not set. Skipping read from file system.")
            return
        }
        log.info("Reading collection configs from path ${collectionPath}.")
        readRecursively(File(collectionPath))
    }

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

    override fun readConfig(file: File): List<CollectionConfig> {
        log.debug("Reading config from file ${file.absolutePath}")
        return readConfig(file.inputStream())
    }

    override fun readConfig(inputStream: InputStream): List<CollectionConfig> {
        val configs = yamlMapper.readValues(YAMLFactory().createParser(inputStream), CollectionConfig::class.java)
            .readAll()

        configs.forEach { readSingleConfig(it) }
        return configs
    }

    override fun readConfigsFromDatabase() {
        log.info("Reading all configs from database")
        configPersistenceService.findConfigs()
            .filterNotNull()
            .forEach { readConfig(ByteArrayInputStream(it.toByteArray())) }
    }

    private fun readSingleConfig(config: CollectionConfig) {
        if (!collectionNameRegex.matches(config.collection)) {
            throw ConfigException("Collection name ${config.collection} must match regex ${collectionNameRegex.pattern}")
        }
        if (!config.retention.isNullOrBlank()) {
            try {
                val retentionDuration = Duration.parse(config.retention)
                val retentionDurationHumanReadable = formatDurationHMS(retentionDuration.toMillis())
                log.info("Collection config ${config.collection} with retention of $retentionDurationHumanReadable")
            } catch (ex: Exception) {
                throw ConfigException("Retention duration ${config.retention} of collection name ${config.collection} is not a valid ISO-8601 format")
            }
        }

        log.info("Adding config ${config.collection}")
        try {
            configPersistenceService.saveConfig(config.collection, yamlMapper.writeValueAsString(config))
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ConfigException("Unable to save config ${config.collection} in database: ${ex.message}")
        }
        configCache.put(config.collection, config)
    }


    override fun getConfig(name: String): CollectionConfig {
        return loadConfig(name) ?: throw ConfigException("Config with name $name not found")
    }

    override fun getConfigs(): Collection<CollectionConfig> {
        return configPersistenceService.findConfigs()
            .filterNotNull()
            .flatMap { parseConfig(ByteArrayInputStream(it.toByteArray())) }
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

    override fun parseConfig(inputStream: InputStream): List<CollectionConfig> {
        return yamlMapper.readValues(YAMLFactory().createParser(inputStream), CollectionConfig::class.java)
            .readAll()
    }

    private fun loadConfig(name: String): CollectionConfig? {
        return configCache.get(name, Function {
            val configData = configPersistenceService.findConfig(name) ?: return@Function null
            return@Function yamlMapper.readValue(configData, CollectionConfig::class.java)
        })
    }
}