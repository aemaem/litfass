package lit.fass.litfass.server.persistence.postgresql

import com.fasterxml.jackson.databind.ObjectMapper
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.helper.PostgresSupport
import lit.fass.litfass.server.persistence.PersistenceException
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static lit.fass.litfass.server.persistence.PersistenceService.Companion.ID_KEY

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class PostgresPersistenceServiceSpec extends Specification implements PostgresSupport {

    @Subject
    PostgresqlPersistenceService postgresPersistenceService

    def setup() {
        dropTable("foo")
        postgresPersistenceService = new PostgresqlPersistenceService(jdbcDataSource, new ObjectMapper())
    }

    def "collection and data is saved"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.save(collection, data, null)
        def result = selectAllFromTable(collection)

        then: "data is stored"
        result.size() == 1
        result.getValues("id", String)[0] ==~ /[a-zA-Z0-9].+/
        result.getValues("timestamp", Long)[0]
        result.getValues("data", String)[0] == '{"bar": true, "foo": "bar"}'
        and: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection and data with id is saved"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): "1", foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.save(collection, data, data.id)
        def result = selectAllFromTable(collection)

        then: "data is stored"
        result.size() == 1
        result.getValues("id", String)[0] == "1"
        result.getValues("timestamp", Long)[0]
        result.getValues("data", String)[0] == '{"id": "1", "bar": true, "foo": "bar"}'
        and: "no exception is thrown"
        noExceptionThrown()
    }

    @Unroll
    def "collection and data with id #id throws exception"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): id, foo: "bar", bar: true]

        when: "saved is called"
        postgresPersistenceService.save(collection, data, data.id)

        then: "exception is thrown"
        thrown(PersistenceException)

        where:
        id << [true, 1]
    }
}
