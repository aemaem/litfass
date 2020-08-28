package lit.fass.litfass.server.execution

import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import lit.fass.litfass.server.config.yaml.model.CollectionConfig
import lit.fass.litfass.server.flow.FlowService
import lit.fass.litfass.server.helper.UnitTest.UnitTest
import lit.fass.litfass.server.persistence.CollectionPersistenceService
import lit.fass.litfass.server.persistence.Datastore.POSTGRES
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource


/**
 * @author Michael Mair
 */
@Tag(UnitTest)
@Suppress("unused")
internal class CollectionExecutionServiceTest {

    lateinit var collectionExecutionService: CollectionExecutionService

    @MockK(relaxed = true)
    lateinit var flowServiceMock: FlowService

    @MockK(relaxed = true)
    lateinit var collectionPersistenceServiceMock: CollectionPersistenceService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        collectionExecutionService =
            CollectionExecutionService(flowServiceMock, listOf(collectionPersistenceServiceMock))
    }

    companion object {
        @JvmStatic
        fun `execution calls required services source`() = listOf(
            arguments(
                "single data",
                listOf(mapOf("bar" to true, "foo" to "bar")),
                listOf(mapOf("foo" to "blub"))
            ),
            arguments(
                "list of data",
                listOf(mapOf("bar" to true, "foo" to "bar"), mapOf("bar" to false, "foo" to "bar")),
                listOf(mapOf("foo" to "blub"), mapOf("foo" to "bar"))
            )
        )
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("execution calls required services source")
    fun `execution calls required services`(
        description: String, data: List<Map<String, Any>>, result: List<Map<String, Any>>
    ) {
        every { flowServiceMock.execute(data, any()) } returns result
        every { collectionPersistenceServiceMock.isApplicable(any()) } returns true

        val config = CollectionConfig("foo", null, null, POSTGRES, emptyList())
        collectionExecutionService.execute(config, data)

        verify(exactly = 1) { flowServiceMock.execute(data, any()) }
        verify(exactly = 1) { collectionPersistenceServiceMock.isApplicable(any()) }
        verify(exactly = 1) { collectionPersistenceServiceMock.saveCollection("foo", result) }
        confirmVerified(flowServiceMock, collectionPersistenceServiceMock)
    }

    @Test
    fun `execution calls throws exception when no persistence service is applicable`() {
        val config = CollectionConfig("foo", null, null, POSTGRES, emptyList())
        val data = listOf(mapOf("bar" to true, "foo" to "bar"))

        every { flowServiceMock.execute(data, any()) } returns listOf(mapOf("foo" to "blub"))
        every { collectionPersistenceServiceMock.isApplicable(any()) } returns false

        assertThatExceptionOfType(ExecutionException::class.java).isThrownBy {
            collectionExecutionService.execute(config, data)
        }.withMessage("No persistence service applicable for POSTGRES")

        verify(exactly = 1) { flowServiceMock.execute(data, any()) }
        verify(exactly = 1) { collectionPersistenceServiceMock.isApplicable(any()) }
        verify(exactly = 0) { collectionPersistenceServiceMock.saveCollection("foo", listOf(mapOf("foo" to "blub"))) }
        confirmVerified(flowServiceMock, collectionPersistenceServiceMock)
    }
}