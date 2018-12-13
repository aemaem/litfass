package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.persistence.PersistenceService
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject

import static lit.fass.litfass.server.persistence.Datastore.POSTGRES

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionExecutionServiceSpec extends Specification {

    @Subject
    CollectionExecutionService collectionExecutionService

    ConfigService configServiceMock
    FlowService flowServiceMock
    PersistenceService persistenceServiceMock

    def setup() {
        configServiceMock = Mock()
        flowServiceMock = Mock()
        persistenceServiceMock = Mock()
        collectionExecutionService = new CollectionExecutionService(configServiceMock, flowServiceMock, [persistenceServiceMock])
    }

    def "execution calls required services"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(collection, data)

        then: "all sub services are called"
        1 * configServiceMock.getConfig(collection) >> new CollectionConfig(collection, null, POSTGRES, [])
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> true
        1 * persistenceServiceMock.saveCollection(collection, [foo: "blub"], null)
    }

    def "execution calls required services with given id"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [id: 1, bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(collection, data)

        then: "all sub services are called"
        1 * configServiceMock.getConfig(collection) >> new CollectionConfig(collection, null, POSTGRES, [])
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [id: 1, foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> true
        1 * persistenceServiceMock.saveCollection(collection, [id: 1, foo: "blub"], 1)
    }

    def "execution calls throws exception when no persistence service is applicable"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(collection, data)

        then: "all sub services are called"
        1 * configServiceMock.getConfig(collection) >> new CollectionConfig(collection, null, POSTGRES, [])
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> false
        0 * persistenceServiceMock.saveCollection(*_)
        thrown(ExecutionException)
    }
}
