package lit.fass.server.helper

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration.ofSeconds

/**
 * @author Michael Mair
 */
@TestInstance(PER_CLASS)
abstract class TestcontainerSupport {

    companion object {

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
            withExposedPorts(5432)
            waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*database system is ready to accept connections.*")
                    .withTimes(2)
                    .withStartupTimeout(ofSeconds(10))
            )
        }

        val litfassServer = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("CONFIG_FORCE_litfass_jdbc_url", "jdbc:postgresql://postgres:5432")
            withEnv("CONFIG_FORCE_litfass_jdbc_database", "litfass")
            withEnv("CONFIG_FORCE_litfass_jdbc_username", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_password", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_poolSize", "1")
            addExposedPort(8080)
            withLogConsumer {
                print("SERVER: ${it.utf8String}")
            }
            waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*Server online at .*")
                    .withStartupTimeout(ofSeconds(30))
            )
            dependsOn(postgres)
        }

        init {
            postgres.start()
            litfassServer.start()
        }
    }

    fun baseUrl() = "http://localhost:${litfassServer.getMappedPort(8080)}"
}