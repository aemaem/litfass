package lit.fass.litfass.server.retention

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.helper.LogCapture
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject

import static lit.fass.litfass.server.persistence.Datastore.POSTGRES

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionRetentionServiceTest extends Specification {

    @Subject
    CollectionRetentionService collectionRetentionService

    CollectionPersistenceService persistenceServiceMock

    @Rule
    LogCapture log = new LogCapture()

    def setup() {
        persistenceServiceMock = Mock()
        collectionRetentionService = new CollectionRetentionService([persistenceServiceMock])
        collectionRetentionService.cronExpression = "* * * * * ?"
    }

    def "clean calls persistence service"() {
        given:
        def config = new CollectionConfig("foo", null, "P2D", POSTGRES, [])

        when:
        collectionRetentionService.clean(config)

        then:
        1 * persistenceServiceMock.isApplicable(POSTGRES) >> true
        1 * persistenceServiceMock.deleteBefore("foo", _)
    }

    def "clean throws exception when no persistence service is applicable"() {
        given:
        def config = new CollectionConfig("foo", null, "P2D", POSTGRES, [])

        when:
        collectionRetentionService.clean(config)

        then:
        1 * persistenceServiceMock.isApplicable(POSTGRES) >> false
        0 * persistenceServiceMock.deleteBefore("foo", _)
        thrown(RetentionException)
    }

    def "clean does nothing if no retention duration is given"() {
        given:
        def config = new CollectionConfig("foo", null, null, POSTGRES, [])

        when:
        collectionRetentionService.clean(config)

        then:
        1 * persistenceServiceMock.isApplicable(POSTGRES) >> true
        0 * persistenceServiceMock.deleteBefore("foo", _)
        log.toString().contains("Collection config foo does not have a retention duration defined")
    }
}
