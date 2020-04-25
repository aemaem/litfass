package lit.fass.litfass.server

import lit.fass.config.Profiles.Companion.POSTGRES
import lit.fass.config.Profiles.Companion.TEST
import lit.fass.litfass.server.helper.IntegrationTest.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * @author Michael Mair
 */
@Tag(IntegrationTest)
@SpringBootTest(classes = [ServerApplication::class])
@ActiveProfiles(TEST, POSTGRES)
internal class ServerApplicationTest {

    @Test
    fun `context loads`() {
        assertThat(true).isTrue()
    }
}