package lit.fass.litfass.server.persistence.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import lit.fass.litfass.server.helper.ElasticsearchSupport
import lit.fass.litfass.server.helper.IntegrationTest
import lit.fass.litfass.server.persistence.PersistenceException
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static lit.fass.litfass.server.persistence.PersistenceClient.Companion.ID_KEY

/**
 * @author Michael Mair
 */
@Category(IntegrationTest)
class EsPersistenceClientSpec extends Specification implements ElasticsearchSupport {

    @Subject
    EsPersistenceClient esPersistenceClient

    def setup() {
        cleanDatabase()
        RestHighLevelClient restClient = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))
        esPersistenceClient = new EsPersistenceClient(restClient, new ObjectMapper())
    }

    def "collection and data is saved"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [foo: "bar", bar: true]

        when: "saved is called"
        esPersistenceClient.save(collection, data)

        then: "no exception is thrown"
        noExceptionThrown()
    }

    def "collection and data with id is saved"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): "1", foo: "bar", bar: true]

        when: "saved is called"
        esPersistenceClient.save(collection, data.id, data)

        then: "no exception is thrown"
        noExceptionThrown()
    }

    @Unroll
    def "collection and data with id #id throws exception"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [(ID_KEY): null, foo: "bar", bar: true]

        when: "saved is called"
        esPersistenceClient.save(collection, data.id, data)

        then: "exception is thrown"
        thrown(PersistenceException)

        where:
        id << [null, true, 1]
    }
}
