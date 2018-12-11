package lit.fass.litfass.server.execution

import lit.fass.litfass.server.config.ConfigService
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.persistence.PersistenceService
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject

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
        collectionExecutionService = new CollectionExecutionService(configServiceMock, flowServiceMock, persistenceServiceMock)
    }

    def "execution calls required services"() {
        given: "a collection and data"
        def collection = "foo"
        def data = [bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(collection, data)

        then: "all sub services are called"
        1 * configServiceMock.getConfig(collection) >> new CollectionConfig(collection, [])
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [foo: "blub"]
        1 * persistenceServiceMock.save(collection, [foo: "blub"])
    }

    def "execution calls required services with given id"() {
        given: "a collection and data with id"
        def collection = "foo"
        def data = [id: 1, bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(collection, data)

        then: "all sub services are called"
        1 * configServiceMock.getConfig(collection) >> new CollectionConfig(collection, [])
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [id: 1, foo: "blub"]
        1 * persistenceServiceMock.save(collection, 1, [id: 1, foo: "blub"])
    }
}
