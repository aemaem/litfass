package lit.fass.litfass.server.config.yaml

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * @author Michael Mair
 */
class YamlConfigServiceTest {

    private val yamlConfigService: YamlConfigService = YamlConfigService()

    @Test
    fun readConfigFile() {
        val configFile = this::class.java.getResource("/testCollectionConfig.yml")
        yamlConfigService.readConfig(File(configFile.file))
        val result = yamlConfigService.getConfig("testCollectionConfig.yml")
        assertEquals(result.collection, "foo")
        assertEquals(result.script.language, "kts")
        assertEquals(result.script.code, """listOf(1,2,3).joinToString(":")""")
    }
}