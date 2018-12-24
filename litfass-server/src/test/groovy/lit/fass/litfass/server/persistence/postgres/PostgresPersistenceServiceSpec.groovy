package lit.fass.litfass.server.persistence.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.PostgresSupport
import lit.fass.litfass.server.persistence.PersistenceException
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.OffsetDateTime

import static java.time.ZoneOffset.UTC
import static lit.fass.litfass.server.persistence.CollectionConfigPersistenceService.Companion.COLLECTION_CONFIG_TABLE
import static lit.fass.litfass.server.persistence.CollectionPersistenceService.Companion.ID_KEY
import static org.jooq.impl.DSL.field
import static org.jooq.impl.DSL.table

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class PostgresPersistenceServiceSpec extends Specification implements PostgresSupport {

    @Subject
    PostgresPersistenceService postgresPersistenceService

    def cleanupSpec() {
        jdbcDataSource.dataSource.close()
    }

    def setup() {
        dropTable("foo")
        dropTable(COLLECTION_CONFIG_TABLE)
        postgresPersistenceService = new PostgresPersistenceService(jdbcDataSource, new ObjectMapper())
    }

    def "collection and data is saved"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.saveCollection(collection, data, null)
        def result = selectAllFromTable(collection)

        then: "data is stored"
        result.size() == 1
        result.getValues("id", String)[0] ==~ /[a-zA-Z0-9].+/
        result.getValues("data", String)[0] == '{"bar": true, "foo": "bar"}'
        result.getValues("created", OffsetDateTime)[0]
        result.getValues("updated", OffsetDateTime)[0]
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection and data with id is saved"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): "1", foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.saveCollection(collection, data, data.id)
        def result = selectAllFromTable(collection)

        then: "data is stored"
        result.size() == 1
        result.getValues("id", String)[0] == "1"
        result.getValues("data", String)[0] == '{"id": "1", "bar": true, "foo": "bar"}'
        result.getValues("created", OffsetDateTime)[0]
        result.getValues("updated", OffsetDateTime)[0]
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection and data with id is inserted and updated"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): "1", foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.saveCollection(collection, data, data.id)
        and: "saved is called another time with different data and the same id"
        def dataUpdate = [(ID_KEY): "1", foo: "blub", blub: 100]
        postgresPersistenceService.saveCollection(collection, dataUpdate, dataUpdate.id)
        def result = selectAllFromTable(collection)

        then: "data is merged"
        result.size() == 1
        result.getValues("id", String)[0] == "1"
        result.getValues("data", String)[0] == '{"id": "1", "bar": true, "foo": "blub", "blub": 100}'
        result.getValues("created", OffsetDateTime)[0]
        result.getValues("updated", OffsetDateTime)[0]
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection is deleted before a given timestamp"() {
        given: "collection data in database"
        def now = OffsetDateTime.now(UTC)
        postgresPersistenceService.saveCollection("foo", [foo: "bar"], "1")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(1))
                .where(field("id").eq("1")).execute()
        postgresPersistenceService.saveCollection("foo", [foo: "bar"], "2")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(2))
                .where(field("id").eq("2")).execute()
        postgresPersistenceService.saveCollection("foo", [foo: "bar"], "3")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(3))
                .where(field("id").eq("3")).execute()
        postgresPersistenceService.saveCollection("foo", [foo: "bar"], "4")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(4))
                .where(field("id").eq("4")).execute()
        postgresPersistenceService.saveCollection("foo", [foo: "bar"], "5")
        jooq.update(table("foo")).set(field("updated"), now.minusDays(5))
                .where(field("id").eq("5")).execute()

        when: "delete before is called"
        postgresPersistenceService.deleteBefore("foo", now.minusHours(25))
        def result = selectAllFromTable("foo")

        then: "data is stored"
        result.size() == 1
        result.getValues("id", String)[0] == "1"
        and: "no exception is thrown"
        noExceptionThrown()
    }

    @Unroll
    def "collection and data with id #id throws exception"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): id, foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.saveCollection(collection, data, data.id)

        then: "exception is thrown"
        thrown(PersistenceException)

        where:
        id << [true, 1]
    }

    def "collection config is saved"() {
        given: "a collection config"
        def collection = "foo"
        def config = """
        collection: $collection
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()

        when: "saved is called"
        postgresPersistenceService.saveConfig(collection, config)
        def result = selectAllFromTable(COLLECTION_CONFIG_TABLE)

        then: "config is stored"
        result.size() == 1
        result.getValues("collection", String)[0] == "foo"
        result.getValues("config", String)[0] == """
        collection: foo
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        result.getValues("created", OffsetDateTime)[0]
        result.getValues("updated", OffsetDateTime)[0]
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection config is found"() {
        given: "a collection config"
        def collection = "foo"
        def config = """
        collection: $collection
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        postgresPersistenceService.saveConfig(collection, config)

        when: "find is called"
        def result = postgresPersistenceService.findConfig("foo")

        then: "config is found"
        result == """
        collection: foo
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "all collection configs are found"() {
        given: "several collection configs"
        postgresPersistenceService.saveConfig("foo1", """
        collection: foo1
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent())
        postgresPersistenceService.saveConfig("foo2", """
        collection: foo2
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent())
        postgresPersistenceService.saveConfig("foo3", """
        collection: foo3
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent())

        when: "find all is called"
        def result = postgresPersistenceService.findConfigs()

        then: "configs are found"
        result.size() == 3
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection config is deleted"() {
        given: "a collection config"
        def collection = "foo"
        def config = """
        collection: $collection
        flows:
          - flow:
              steps:
                - script:
                    description: "Transform something"
                    extension: kts
                    code: bindings["data"]
        """.stripIndent()
        postgresPersistenceService.saveConfig(collection, config)

        when: "delete is called"
        postgresPersistenceService.deleteConfig("foo")
        def result = selectAllFromTable(COLLECTION_CONFIG_TABLE)

        then: "config is found"
        result.isEmpty()
        and: "no exception is thrown"
        noExceptionThrown()
    }
}
