package lit.fass.server.security

import com.typesafe.config.ConfigFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


/**
 * @author Michael Mair
 */
internal class SecurityManagerTest {

    val classUnderTest = SecurityManager(ConfigFactory.parseResources("application-test.conf"))

    @Test
    fun `security manager initialize users from config`() {
        assertThat(classUnderTest.getRealm().accountExists("admin")).isTrue()
        assertThat(classUnderTest.getRealm().accountExists("user")).isTrue()
        assertThat(classUnderTest.getRealm().accountExists("foo")).isFalse()
    }

    @Test
    fun `security manager authenticates users`() {
        val subject = classUnderTest.loginHttpBasic("YWRtaW46YWRtaW4=") // admin:admin
        assertThat(subject.isAuthenticated).isTrue()
    }

    @Test
    fun `security manager does not authenticate users with wrong password`() {
        val subject = classUnderTest.loginHttpBasic("YWRtaW46QURNSU4=") // admin:ADMIN
        assertThat(subject.isAuthenticated).isFalse()
    }

    @Test
    fun `security manager authorizes users`() {
        val subject = classUnderTest.loginHttpBasic("YWRtaW46YWRtaW4=") // admin:admin
        assertThat(subject.hasRole("ADMIN")).isTrue()
        assertThat(subject.hasRole("FOO")).isFalse()
    }

}