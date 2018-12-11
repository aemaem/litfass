package lit.fass.litfass.server.schedule

import lit.fass.litfass.server.execution.ExecutionService
import lit.fass.litfass.server.helper.LogCapture
import lit.fass.litfass.server.helper.UnitTest
import org.junit.Rule
import org.junit.experimental.categories.Category
import spock.lang.Specification
import spock.lang.Subject

import static java.util.concurrent.TimeUnit.SECONDS
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.with

/**
 * @author Michael Mair
 */
@Category(UnitTest)
class CollectionSchedulerServiceSpec extends Specification {

    @Subject
    CollectionSchedulerService collectionSchedulerService

    ExecutionService executionServiceMock

    @Rule
    LogCapture log = new LogCapture()

    def setup() {
        executionServiceMock = Mock()
        collectionSchedulerService = new CollectionSchedulerService(executionServiceMock)
    }

    def "scheduled job is created"() {
        given: "a collection and a cron expression"
        def collection = "foo"
        def cronExpression = "* * * * * * *" // every second

        when: "job is created"
        collectionSchedulerService.createJob(collection, cronExpression)

        then: "creation logs are printed"
        log.toString().contains("Creating scheduled job foo with cron * * * * * * *")
        log.toString().contains("Sending job foo to be scheduled every second")
        and: "scheduler scheduled job"
        await().until { log.toString().contains("Scheduled job foo") }
        await().until { log.toString().contains("Executed job foo") }
    }

    def "job creation throws exception when expression is not valid"() {
        given: "a collection and a wrong cron expression"
        def collection = "foo"
        def cronExpression = "99 * * * * * *"

        when: "job is created"
        collectionSchedulerService.createJob(collection, cronExpression)

        then: "creation logs are printed"
        thrown(SchedulerException)
    }

    def "job cannot be created if it already exists"() {
        given: "a collection and a cron expression"
        def collection = "foo"
        def cronExpression = "0 0 * * * * *" // every hour

        when: "job is created"
        collectionSchedulerService.createJob(collection, cronExpression)
        and: "the same job should be created again"
        collectionSchedulerService.createJob(collection, "* * * * * * *")

        then: "creation logs are printed"
        log.toString().contains("Creating scheduled job foo with cron * * * * * * *")
        log.toString().contains("Sending job foo to be scheduled every second")
        and: "scheduler does not schedule the job"
        await().until { log.toString().contains("Scheduled job foo already exists. Job won't be scheduled") }
    }

    def "job is cancelled immediately if next execution is in the past"() {
        given: "a collection and a cron expression"
        def collection = "foo"
        def cronExpression = "0 0 * * * * 2016" // every hour 2016

        when: "job is created"
        collectionSchedulerService.createJob(collection, cronExpression)

        then: "creation logs are printed"
        log.toString().contains("Creating scheduled job foo with cron 0 0 * * * * 2016")
        log.toString().contains("Sending job foo to be scheduled every hour at 2016 year")
        and: "scheduler immediately cancels the job"
        await().until { log.toString().contains("Job foo cancelled because there is no upcoming execution") }
    }

    def "job can be cancelled"() {
        given: "scheduled job"
        def collection = "foo"
        def cronExpression = "* * * * * * *" // every second
        collectionSchedulerService.createJob(collection, cronExpression)
        with().pollDelay(2, SECONDS).await().until { true }

        when: "job is cancelled"
        collectionSchedulerService.cancelJob(collection)

        then: "cancellation logs are printed"
        log.toString().contains("Sending job foo to be cancelled")
        and: "scheduler cancels the job"
        await().until { log.toString().contains("Cancelled scheduled job foo") }
    }
}
