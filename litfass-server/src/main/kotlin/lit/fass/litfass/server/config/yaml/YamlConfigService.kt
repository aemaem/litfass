package lit.fass.litfass.server.config.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.cache.CacheBuilder
import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.persistence.CollectionConfigPersistenceService
import lit.fass.litfass.server.schedule.SchedulerService
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.util.concurrent.Callable

/**
 * @author Michael Mair
 */
class YamlConfigService(
    private val configPersistenceService: CollectionConfigPersistenceService,
    private val schedulerService: SchedulerService
) : ConfigService {
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

    override fun readConfig(file: File) {
        log.debug("Reading config from file ${file.absolutePath}")
        return readConfig(file.inputStream())
    }

    override fun readConfig(inputStream: InputStream) {
        val configs = yamlMapper.readValues(YAMLFactory().createParser(inputStream), CollectionConfig::class.java)
            .readAll()

        configs.forEach { readSingleConfig(it) }
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
            scheduleConfig(config)
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
        return configCache.asMap().values
    }

    override fun removeConfig(name: String) {
        log.info("Removing config $name")
        val config = loadConfig(name) ?: return
        try {
            configPersistenceService.deleteConfig(name)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ConfigException("Unable to delete config $name in database")
        }
        schedulerService.cancelCollectionJob(config)
        schedulerService.cancelRetentionJob(config)
        configCache.invalidate(name)
    }

    private fun loadConfig(name: String): CollectionConfig? {
        return configCache.get(name, Callable {
            val configData = configPersistenceService.findConfig(name) ?: return@Callable null
            return@Callable yamlMapper.readValue(configData, CollectionConfig::class.java)
        })
    }

    private fun scheduleConfig(config: CollectionConfig) {
        if (config.scheduled != null) {
            try {
                schedulerService.createCollectionJob(config)
            } catch (ex: Exception) {
                log.error("Unable to schedule config ${config.collection}", ex)
                throw ConfigException("Unable to schedule collection config ${config.collection}: ${ex.message}")
            }
        }
        if (config.retention != null) {
            try {
                schedulerService.createRetentionJob(config)
            } catch (ex: Exception) {
                log.error("Unable to schedule config ${config.collection}", ex)
                throw ConfigException("Unable to schedule retention config ${config.collection}: ${ex.message}")
            }
        }
    }
}