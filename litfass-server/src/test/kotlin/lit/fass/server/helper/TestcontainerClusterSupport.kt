package lit.fass.server.helper

import com.github.kittinunf.fuel.core.FuelManager
import lit.fass.server.persistence.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.impl.DSL.table
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration.ofSeconds

/**
 * @author Michael Mair
 */
abstract class TestcontainerClusterSupport {

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

        val litfassServer1 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8080")
            withEnv("LITFASS_AKKA_REMOTE_HOSTNAME", "litfass1")
            withEnv("LITFASS_AKKA_REMOTE_PORT", "25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            withEnv("CONFIG_FORCE_litfass_jdbc_url", "jdbc:postgresql://postgres:5432")
            withEnv("CONFIG_FORCE_litfass_jdbc_database", "litfass")
            withEnv("CONFIG_FORCE_litfass_jdbc_username", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_password", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_poolSize", "1")
            addExposedPort(8080)
            withNetworkAliases("litfass1")
            withLogConsumer {
                print("SERVER-1: ${it.utf8String}")
            }
            waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*Server online at .*")
                    .withStartupTimeout(ofSeconds(30))
            )
            dependsOn(postgres)
        }
        val litfassServer2 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8081")
            withEnv("LITFASS_AKKA_REMOTE_HOSTNAME", "litfass2")
            withEnv("LITFASS_AKKA_REMOTE_PORT", "25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            withEnv("CONFIG_FORCE_litfass_jdbc_url", "jdbc:postgresql://postgres:5432")
            withEnv("CONFIG_FORCE_litfass_jdbc_database", "litfass")
            withEnv("CONFIG_FORCE_litfass_jdbc_username", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_password", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_poolSize", "1")
            addExposedPort(8081)
            withNetworkAliases("litfass2")
            withLogConsumer {
                print("SERVER-2: ${it.utf8String}")
            }
            waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*Server online at .*")
                    .withStartupTimeout(ofSeconds(30))
            )
            dependsOn(postgres, litfassServer1)
        }
        val litfassServer3 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8082")
            withEnv("LITFASS_AKKA_REMOTE_HOSTNAME", "litfass3")
            withEnv("LITFASS_AKKA_REMOTE_PORT", "25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            withEnv("CONFIG_FORCE_litfass_jdbc_url", "jdbc:postgresql://postgres:5432")
            withEnv("CONFIG_FORCE_litfass_jdbc_database", "litfass")
            withEnv("CONFIG_FORCE_litfass_jdbc_username", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_password", "admin")
            withEnv("CONFIG_FORCE_litfass_jdbc_poolSize", "1")
            addExposedPort(8082)
            withNetworkAliases("litfass3")
            withLogConsumer {
                print("SERVER-3: ${it.utf8String}")
            }
            waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*Server online at .*")
                    .withStartupTimeout(ofSeconds(30))
            )
            dependsOn(postgres, litfassServer2)
        }

        init {
            postgres.start()
            litfassServer1.start()
            litfassServer2.start()
            litfassServer3.start()
        }
    }

    lateinit var jdbcDataSource: JdbcDataSource
    lateinit var jooq: DSLContext

    @BeforeAll
    fun setupSpec() {
        jdbcDataSource = JdbcDataSource(
            "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}",
            "litfass",
            "admin",
            "admin",
            1,
            emptyMap()
        )
        jooq = DSL.using(jdbcDataSource.instance(), POSTGRES)
    }

    @AfterAll
    fun cleanupSpec() {
        jdbcDataSource.close()
    }

    fun dropTable(tableName: String) = jooq.dropTableIfExists(tableName).execute()

    fun clearTable(tableName: String) = jooq.delete(table(tableName)).execute()

    fun selectAllFromTable(tableName: String): Result<Record> = jooq.select().from(table(tableName)).fetch()

    fun aggregatedLogs() = litfassServer1.logs + litfassServer2.logs + litfassServer3.logs

    open fun baseUrl1() = "http://localhost:${litfassServer1.getMappedPort(8080)}"
    open fun baseUrl2() = "http://localhost:${litfassServer2.getMappedPort(8081)}"
    open fun baseUrl3() = "http://localhost:${litfassServer3.getMappedPort(8082)}"
}