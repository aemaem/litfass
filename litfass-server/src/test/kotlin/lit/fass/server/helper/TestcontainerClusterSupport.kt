package lit.fass.server.helper

import lit.fass.server.persistence.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.CockroachContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import java.time.Duration.ofMinutes

/**
 * @author Michael Mair
 */
abstract class TestcontainerClusterSupport {

    companion object {

        val testNetwork = Network.newNetwork()

        val cockroach = CockroachContainer("cockroachdb/cockroach:v20.2.3").apply {
            withNetwork(testNetwork)
            withNetworkAliases("cockroach")
            addExposedPort(26257)
            withLogConsumer {
                print("COCKROACH: ${it.utf8String}")
            }
            withCommand("start-single-node --insecure")
        }

        val litfassServer1 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8080")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_HOSTNAME", "litfass1")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_PORT", "25520")
            withEnv("LITFASS_AKKA_REMOTE_BIND_HOSTNAME", "litfass1")
            withEnv("LITFASS_JDBC_URL", "jdbc:postgresql://cockroach:26257")
            withEnv("LITFASS_JDBC_DATABASE", "postgres")
            withEnv("LITFASS_JDBC_USERNAME", "root")
            withEnv("LITFASS_JDBC_PASSWORD", "")
            withEnv("LITFASS_JDBC_POOL_SIZE", "1")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            addExposedPort(8080)
            withNetworkAliases("litfass1")
            withLogConsumer {
                print("SERVER-1: ${it.utf8String}")
            }
            waitingFor(
                HttpWaitStrategy()
                    .forPath("/ready")
                    .forPort(8080)
                    .forStatusCode(200)
                    .withStartupTimeout(ofMinutes(1))
            )
            dependsOn(cockroach)
        }
        val litfassServer2 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8081")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_HOSTNAME", "litfass2")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_PORT", "25520")
            withEnv("LITFASS_AKKA_REMOTE_BIND_HOSTNAME", "litfass2")
            withEnv("LITFASS_JDBC_URL", "jdbc:postgresql://cockroach:26257")
            withEnv("LITFASS_JDBC_DATABASE", "postgres")
            withEnv("LITFASS_JDBC_USERNAME", "root")
            withEnv("LITFASS_JDBC_PASSWORD", "")
            withEnv("LITFASS_JDBC_POOL_SIZE", "1")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            addExposedPort(8081)
            withNetworkAliases("litfass2")
            withLogConsumer {
                print("SERVER-2: ${it.utf8String}")
            }
            waitingFor(
                HttpWaitStrategy()
                    .forPath("/ready")
                    .forPort(8081)
                    .forStatusCode(200)
                    .withStartupTimeout(ofMinutes(1))
            )
            dependsOn(cockroach, litfassServer1)
        }
        val litfassServer3 = GenericContainer<Nothing>("aemaem/litfass:latest").apply {
            withNetwork(testNetwork)
            withEnv("LITFASS_LOG_LEVEL", "DEBUG")
            withEnv("LITFASS_HTTP_PORT", "8082")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_HOSTNAME", "litfass3")
            withEnv("LITFASS_AKKA_REMOTE_CANONICAL_PORT", "25520")
            withEnv("LITFASS_AKKA_REMOTE_BIND_HOSTNAME", "litfass3")
            withEnv("LITFASS_JDBC_URL", "jdbc:postgresql://cockroach:26257")
            withEnv("LITFASS_JDBC_DATABASE", "postgres")
            withEnv("LITFASS_JDBC_USERNAME", "root")
            withEnv("LITFASS_JDBC_PASSWORD", "")
            withEnv("LITFASS_JDBC_POOL_SIZE", "1")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_0", "akka://litfass@litfass1:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_1", "akka://litfass@litfass2:25520")
            withEnv("CONFIG_FORCE_akka_cluster_seed__nodes_2", "akka://litfass@litfass3:25520")
            addExposedPort(8082)
            withNetworkAliases("litfass3")
            withLogConsumer {
                print("SERVER-3: ${it.utf8String}")
            }
            waitingFor(
                HttpWaitStrategy()
                    .forPath("/ready")
                    .forPort(8082)
                    .forStatusCode(200)
                    .withStartupTimeout(ofMinutes(1))
            )
            dependsOn(cockroach, litfassServer2)
        }

        init {
            cockroach.start()
            litfassServer1.start()
            litfassServer2.start()
            litfassServer3.start()
            Thread.sleep(30_000)
        }
    }

    lateinit var jdbcDataSource: JdbcDataSource
    lateinit var jooq: DSLContext

    @BeforeAll
    fun setupSpec() {
        jdbcDataSource = JdbcDataSource(
            "jdbc:postgresql://${cockroach.host}:${cockroach.getMappedPort(26257)}",
            "postgres",
            "root",
            "",
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

    fun selectAllFromTable(tableName: String): Result<Record> = jooq.select().from(table(tableName)).orderBy(field("created")).fetch()

    fun aggregatedLogs() = litfassServer1.logs + litfassServer2.logs + litfassServer3.logs

    open fun baseUrl1() = "http://localhost:${litfassServer1.getMappedPort(8080)}"
    open fun baseUrl2() = "http://localhost:${litfassServer2.getMappedPort(8081)}"
    open fun baseUrl3() = "http://localhost:${litfassServer3.getMappedPort(8082)}"
}