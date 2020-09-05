package lit.fass.server.retention

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.server.config.yaml.model.CollectionConfig
import lit.fass.server.helper.UnitTest
import lit.fass.server.helper.UnitTest.UnitTest
import lit.fass.server.persistence.CollectionPersistenceService
import lit.fass.server.persistence.Datastore.POSTGRES
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

/**
 * @author Michael Mair
 */
@Tag(UnitTest)
@ExtendWith(OutputCaptureExtension::class)
internal class CollectionRetentionServiceTest {

    lateinit var collectionRetentionService: CollectionRetentionService

    @MockK(relaxed = true)
    lateinit var persistenceServiceMock: CollectionPersistenceService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        collectionRetentionService = CollectionRetentionService(listOf(persistenceServiceMock))
    }

    @Test
    fun `clean calls persistence service`() {
        val config = CollectionConfig("foo", null, "P2D", POSTGRES, emptyList())
        every { persistenceServiceMock.isApplicable(POSTGRES) } returns true

        collectionRetentionService.clean(config)

        verify(exactly = 1) { persistenceServiceMock.isApplicable(POSTGRES) }
        verify(exactly = 1) { persistenceServiceMock.deleteBefore("foo", any()) }
        confirmVerified(persistenceServiceMock)
    }

    @Test
    fun `clean throws exception when no persistence service is applicable`() {
        val config = CollectionConfig("foo", null, "P2D", POSTGRES, emptyList())
        every { persistenceServiceMock.isApplicable(POSTGRES) } returns false

        assertThatThrownBy { collectionRetentionService.clean(config) }
            .isInstanceOf(RetentionException::class.java)

        verify(exactly = 1) { persistenceServiceMock.isApplicable(POSTGRES) }
        confirmVerified(persistenceServiceMock)
    }

    @Test
    fun `clean does nothing if no retention duration is given`(output: CapturedOutput) {
        val config = CollectionConfig("foo", null, null, POSTGRES, emptyList())
        every { persistenceServiceMock.isApplicable(POSTGRES) } returns true

        collectionRetentionService.clean(config)

        assertThat(output).contains("Collection config foo does not have a retention duration defined")
        verify(exactly = 1) { persistenceServiceMock.isApplicable(POSTGRES) }
        confirmVerified(persistenceServiceMock)
    }
}