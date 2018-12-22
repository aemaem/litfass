package lit.fass.litfass.server.execution


import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.persistence.CollectionPersistenceService
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

    FlowService flowServiceMock
    CollectionPersistenceService persistenceServiceMock

    def setup() {
        flowServiceMock = Mock()
        persistenceServiceMock = Mock()
        collectionExecutionService = new CollectionExecutionService(flowServiceMock, [persistenceServiceMock])
    }

    def "execution calls required services"() {
        given: "a config and data"
        def config = new CollectionConfig("foo", null, null, POSTGRES, [])
        def data = [bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(config, data)

        then: "all sub services are called"
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> true
        1 * persistenceServiceMock.saveCollection("foo", [foo: "blub"], null)
    }

    def "execution calls required services with given id"() {
        given: "a config and data with id"
        def config = new CollectionConfig("foo", null, null, POSTGRES, [])
        def data = [id: 1, bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(config, data)

        then: "all sub services are called"
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [id: 1, foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> true
        1 * persistenceServiceMock.saveCollection("foo", [id: 1, foo: "blub"], 1)
    }

    def "execution calls throws exception when no persistence service is applicable"() {
        given: "a config and data"
        def config = new CollectionConfig("foo", null, null, POSTGRES, [])
        def data = [bar: true, foo: "bar"]

        when: "execution service is called"
        collectionExecutionService.execute(config, data)

        then: "all sub services are called"
        1 * flowServiceMock.execute(data, _ as CollectionConfig) >> [foo: "blub"]
        1 * persistenceServiceMock.isApplicable(_) >> false
        0 * persistenceServiceMock.saveCollection(*_)
        thrown(ExecutionException)
    }
}
