package lit.fass.server.helper

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer

/**
 * @author Michael Mair
 */
@TestInstance(PER_CLASS)
abstract class End2EndSupport {

    val testNetwork = Network.newNetwork()

    val postgres = PostgreSQLContainer<Nothing>("${PostgreSQLContainer.IMAGE}:11.1-alpine").apply {
        withNetwork(testNetwork)
        withDatabaseName("litfass")
        withUsername("admin")
        withPassword("admin")
        withLogConsumer {
            print("POSTGRES: ${it.utf8String}")
        }
        withNetworkAliases("postgres")
    }

    val litfassServer = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
        withNetwork(testNetwork)
        addExposedPort(8080)
        withLogConsumer {
            print("SERVER: ${it.utf8String}")
        }
    }

    @BeforeAll
    fun setupClass() {
        postgres.start()
        litfassServer.start()
    }

    @AfterAll
    fun cleanupClass() {
        litfassServer.stop()
        postgres.stop()
    }

    fun baseUrl() = "http://${litfassServer.host}:${litfassServer.getMappedPort(8080)}"
}