package lit.fass.litfass.server.config

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import java.io.File
import java.io.InputStream

/**
 * @author Michael Mair
 */
interface ConfigService {
    fun readRecursively(file: File)
    fun readConfig(file: File)
    fun readConfig(inputStream: InputStream)
    fun getConfig(name: String): CollectionConfig
    fun getConfigs(): Collection<CollectionConfig>
    fun removeConfig(name: String)
}
