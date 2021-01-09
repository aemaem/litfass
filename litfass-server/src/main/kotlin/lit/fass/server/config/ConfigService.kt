package lit.fass.server.config

import lit.fass.server.config.yaml.model.CollectionConfig
import java.io.File
import java.io.InputStream

/**
 * @author Michael Mair
 */
interface ConfigService {
    fun readRecursively(file: File)
    fun readConfig(file: File): List<CollectionConfig>
    fun readConfig(inputStream: InputStream): List<CollectionConfig>
    fun readConfigsFromDatabase()
    fun parseConfig(inputStream: InputStream): List<CollectionConfig>
    fun getConfig(name: String): CollectionConfig
    fun getConfigs(): Collection<CollectionConfig>
    fun removeConfig(name: String)
}
