package lit.fass.litfass.server.config

import lit.fass.litfass.server.config.yaml.CollectionConfig
import java.io.File
import java.io.InputStream

interface ConfigService {
    fun readConfig(file: File)
    fun readConfig(name: String, inputStream: InputStream)
    fun getConfig(name: String): CollectionConfig
}
