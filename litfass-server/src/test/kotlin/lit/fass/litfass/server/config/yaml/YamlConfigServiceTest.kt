package lit.fass.litfass.server.config.yaml

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * @author Michael Mair
 */
//todo: use spek https://spekframework.org/
class YamlConfigServiceTest {

    private val yamlConfigService: YamlConfigService = YamlConfigService()

    @Test
    fun readConfigFile() {
        val configFile = this::class.java.getResource("/config/yaml/testCollectionConfig.yml")
        yamlConfigService.readConfig(File(configFile.file))
        val result = yamlConfigService.getConfig("foo")
        assertEquals(result.collection, "foo")
        assertEquals(result.flow.size, 3)
        val component1 = result.flow[0] as CollectionComponentTransformConfig
        assertEquals(component1.description, "First step")
        assertEquals(component1.language, "kts")
        assertEquals(component1.code, """println("foo")""")
        val component2 = result.flow[1] as CollectionComponentRequestConfig
        assertEquals(component2.description, "Second step")
        assertEquals(component2.url, "https://some.url/foo?bar=true")
        assertEquals(component2.username, "user")
        assertEquals(component2.password, "secret")
        val component3 = result.flow[2] as CollectionComponentTransformConfig
        assertEquals(component3.description, null)
        assertEquals(component3.language, "kts")
        assertEquals(component3.code, """println("bar")""")
    }

    //todo: test file recursive read
}