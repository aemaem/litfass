package lit.fass.litfass.server.schedule

import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.helper.LogCapture
import lit.fass.litfass.server.helper.UnitTest
import lit.fass.litfass.server.retention.RetentionService
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject
import spock.util.concurrent.BlockingVariable

import static java.util.concurrent.TimeUnit.SECONDS
import static lit.fass.litfass.server.persistence.Datastore.POSTGRES
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.with

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class QuartzCollectionSchedulerServiceSpec extends Specification {

    @Subject
    QuartzCollectionSchedulerService collectionSchedulerService

    ExecutionService executionServiceMock
    RetentionService retentionServiceMock

    @Rule
    LogCapture log = new LogCapture()

    def setup() {
        executionServiceMock = Mock()
        retentionServiceMock = Mock()
        collectionSchedulerService = new QuartzCollectionSchedulerService(executionServiceMock, retentionServiceMock)
    }

    def cleanup() {
        collectionSchedulerService.stop()
    }

    def "scheduled collection job is created"() {
        given: "a config and a cron expression"
        def config = new CollectionConfig("foo", "* * * * * ? *", null, POSTGRES, [])
        def executionServiceCalled = new BlockingVariable<Boolean>(5)
        (1.._) * executionServiceMock.execute(config, _) >> { args ->
            assert args[1].containsKey("timestamp")
            executionServiceCalled.set(true)
        }

        when: "job is created"
        collectionSchedulerService.createCollectionJob(config)

        then: "creation logs are printed"
        log.toString().contains("Creating scheduled collection job foo with cron * * * * * ? *")
        log.toString().contains("Collection job foo to be scheduled every second")
        and: "execution service has been called at least once"
        executionServiceCalled.get()
        and: "scheduler scheduled job"
        await().until { log.toString().contains("Executed collection job foo") }
    }

    def "collection job creation throws exception when expression is not valid"() {
        given: "a config and a wrong cron expression"
        def config = new CollectionConfig("foo", "99 * * * * ? *", null, POSTGRES, [])

        when: "job is created"
        collectionSchedulerService.createCollectionJob(config)

        then: "creation logs are printed"
        0 * executionServiceMock._
        thrown(SchedulerException)
    }

    def "collection job is overwritten if it already exists"() {
        given: "a config and a cron expression"
        def config = new CollectionConfig("foo", "0 0 * * * ? *", null, POSTGRES, [])

        when: "job is created"
        collectionSchedulerService.createCollectionJob(config)
        and: "the same job should be created again"
        collectionSchedulerService.createCollectionJob(new CollectionConfig("foo", "* * * * * ? *", null, POSTGRES, []))

        then: "scheduler does schedule the job"
        noExceptionThrown()
    }

    def "collection job is cancelled immediately if next execution is in the past"() {
        given: "a config and a cron expression"
        def config = new CollectionConfig("foo", "0 0 * * * ? 2016", null, POSTGRES, [])

        when: "job is created"
        collectionSchedulerService.createCollectionJob(config)

        then: "execution service has never been called"
        0 * executionServiceMock._
        thrown(org.quartz.SchedulerException)
    }

    def "collection job can be cancelled"() {
        given: "scheduled job"
        def config = new CollectionConfig("foo", "* * * * * ? *", null, POSTGRES, [])
        collectionSchedulerService.createCollectionJob(config)
        with().pollDelay(2, SECONDS).await().until { true }

        when: "job is cancelled"
        collectionSchedulerService.cancelCollectionJob(config)

        then: "cancellation logs are printed"
        log.toString().contains("Collection job foo to be cancelled")
    }

    def "scheduled retention job is created"() {
        given: "a config and a retention duration"
        def config = new CollectionConfig("foo", null, "P2D", POSTGRES, [])
        def retentionServiceCalled = new BlockingVariable<Boolean>(5)
        (2..3) * retentionServiceMock.getCronExpression() >> "* * * * * ? *"
        (1.._) * retentionServiceMock.clean(config) >> { args ->
            retentionServiceCalled.set(true)
        }

        when: "job is created"
        collectionSchedulerService.createRetentionJob(config)

        then: "creation logs are printed"
        log.toString().contains("Creating scheduled retention job foo with cron * * * * * ? *")
        log.toString().contains("Retention job foo to be scheduled every second")
        and: "retention service has been called at least once"
        retentionServiceCalled.get()
        and: "scheduler scheduled job"
        await().until { log.toString().contains("Executed retention job foo") }
    }

    def "retention job is overwritten if it already exists"() {
        given: "a config and a retention duration"
        def config = new CollectionConfig("foo", null, "P2D", POSTGRES, [])
        (4..6) * retentionServiceMock.getCronExpression() >> "* * * * * ? *"

        when: "job is created"
        collectionSchedulerService.createRetentionJob(config)
        and: "the same job should be created again"
        collectionSchedulerService.createRetentionJob(new CollectionConfig("foo", null, "P3D", POSTGRES, []))

        then: "scheduler does schedule the job"
        noExceptionThrown()
    }

    def "retention job can be cancelled"() {
        given: "scheduled job"
        def config = new CollectionConfig("foo", null, "P2D", POSTGRES, [])
        (2..3) * retentionServiceMock.getCronExpression() >> "* * * * * ? *"
        collectionSchedulerService.createRetentionJob(config)
        with().pollDelay(2, SECONDS).await().until { true }

        when: "job is cancelled"
        collectionSchedulerService.cancelRetentionJob(config)

        then: "cancellation logs are printed"
        log.toString().contains("Retention job foo to be cancelled")
    }

}
