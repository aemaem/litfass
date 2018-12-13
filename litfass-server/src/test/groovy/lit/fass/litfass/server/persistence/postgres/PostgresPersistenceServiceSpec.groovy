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

import static lit.fass.litfass.server.persistence.PersistenceService.Companion.ID_KEY

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class PostgresPersistenceServiceSpec extends Specification implements PostgresSupport {

    @Subject
    PostgresPersistenceService postgresPersistenceService

    def setup() {
        dropTable("foo")
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
}
