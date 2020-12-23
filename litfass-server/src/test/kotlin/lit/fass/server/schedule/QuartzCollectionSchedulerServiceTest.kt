package lit.fass.server.schedule

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.execution.ExecutionService
import lit.fass.server.helper.TestTypes.UnitTest
import lit.fass.server.persistence.Datastore.POSTGRES
import lit.fass.server.retention.RetentionService
import org.assertj.core.api.Assertions.*
import org.awaitility.Awaitility.await
import org.awaitility.Awaitility.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Michael Mair
 */
@Tag(UnitTest)
@ExtendWith(OutputCaptureExtension::class)
internal class QuartzCollectionSchedulerServiceTest {

    lateinit var collectionSchedulerService: QuartzCollectionSchedulerService

    @MockK(relaxed = true)
    lateinit var executionServiceMock: ExecutionService

    @MockK(relaxed = true)
    lateinit var retentionServiceMock: RetentionService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        collectionSchedulerService = QuartzCollectionSchedulerService(executionServiceMock, retentionServiceMock)
    }

    @AfterEach
    fun cleanup() {
        collectionSchedulerService.stop()
    }

    @Test
    fun `scheduled collection job is created`(output: CapturedOutput) {
        val config = CollectionConfig("foo", "* * * * * ? *", null, POSTGRES, emptyList())
        val executionServiceCalled = AtomicBoolean(false)
        every { executionServiceMock.execute(config, any()) } answers {
            @Suppress("UNCHECKED_CAST")
            assertThat((args[1] as Collection<Map<String, Any?>>).first()).containsKey("timestamp")
            executionServiceCalled.set(true)
        }

        collectionSchedulerService.createCollectionJob(config)

        assertThat(output).contains(
            "Creating scheduled collection job foo with cron * * * * * ? *",
            "Collection job foo to be scheduled every second"
        )
        await().until { executionServiceCalled.get() }
        await().until { output.contains("Executed collection job foo") }
    }

    @Test
    fun `collection job creation throws exception when expression is not valid`() {
        val config = CollectionConfig("foo", "99 * * * * ? *", null, POSTGRES, emptyList())
        assertThatThrownBy { collectionSchedulerService.createCollectionJob(config) }
            .isInstanceOf(SchedulerException::class.java)
        verify(exactly = 0) { executionServiceMock.execute(any(), any()) }
        confirmVerified(executionServiceMock)
    }

    @Test
    fun `collection job is overwritten if it already exists`() {
        val config = CollectionConfig("foo", "0 0 * * * ? *", null, POSTGRES, emptyList())
        assertThatCode {
            collectionSchedulerService.createCollectionJob(config)
            collectionSchedulerService.createCollectionJob(
                CollectionConfig(
                    "foo",
                    "* * * * * ? *",
                    null,
                    POSTGRES,
                    emptyList()
                )
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `collection job is cancelled immediately if next execution is in the past`() {
        val config = CollectionConfig("foo", "0 0 * * * ? 2016", null, POSTGRES, emptyList())
        assertThatThrownBy { collectionSchedulerService.createCollectionJob(config) }
            .isInstanceOf(org.quartz.SchedulerException::class.java)
        verify(exactly = 0) { executionServiceMock.execute(any(), any()) }
        confirmVerified(executionServiceMock)
    }

    @Test
    fun `collection job can be cancelled`(output: CapturedOutput) {
        val config = CollectionConfig("foo", "* * * * * ? *", null, POSTGRES, emptyList())
        collectionSchedulerService.createCollectionJob(config)
        with().pollDelay(2, SECONDS).await().until { true }
        collectionSchedulerService.cancelCollectionJob(config)
        assertThat(output).contains("Collection job foo to be cancelled")
    }

    @Test
    fun `scheduled retention job is created`(output: CapturedOutput) {
        val config = CollectionConfig("foo", null, "P2D", POSTGRES, emptyList())

        val retentionServiceCalled = AtomicBoolean(false)
        every { retentionServiceMock.getCronExpression() } returns "* * * * * ? *"
        every { retentionServiceMock.clean(config) } answers { retentionServiceCalled.set(true) }

        collectionSchedulerService.createRetentionJob(config)

        assertThat(output).contains(
            "Creating scheduled retention job foo with cron * * * * * ? *",
            "Retention job foo to be scheduled every second"
        )
        await().until { retentionServiceCalled.get() }
        await().until { output.contains("Executed retention job foo") }

        verify(atLeast = 2, atMost = 3) { retentionServiceMock.getCronExpression() }
        verify(atLeast = 1) { retentionServiceMock.clean(config) }
        confirmVerified(retentionServiceMock)
    }

    @Test
    fun `retention job can be cancelled`(output: CapturedOutput) {
        val config = CollectionConfig("foo", null, "P2D", POSTGRES, emptyList())
        every { retentionServiceMock.getCronExpression() } returns "* * * * * ? *"

        collectionSchedulerService.createRetentionJob(config)
        with().pollDelay(2, SECONDS).await().until { true }
        collectionSchedulerService.cancelRetentionJob(config)

        assertThat(output).contains("Retention job foo to be cancelled")
        verify(atLeast = 2, atMost = 3) { retentionServiceMock.getCronExpression() }
        verify(atMost = 3) { retentionServiceMock.clean(config) }
        confirmVerified(retentionServiceMock)
    }
}