package lit.fass.server.helper

import com.github.kittinunf.fuel.core.FuelManager
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

/**
 * @author Michael Mair
 */
@TestInstance(PER_CLASS)
abstract class LocalhostSupport {

    @BeforeAll
    fun setupSpec() {
        FuelManager.instance.basePath = baseUrl()
    }

    open fun baseUrl() = "http://localhost:8080"
}