package lit.fass.server.security

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import lit.fass.server.logger
import org.apache.shiro.authc.SimpleAccount
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.codec.Base64
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.subject.Subject

/**
 * @author Michael Mair
 */
class SecurityManager(config: Config = ConfigFactory.defaultApplication()) : DefaultSecurityManager(SecurityRealm()) {

    companion object {
        private val log = this.logger()
    }

    init {
        initFromConfig(config)
    }

    fun buildSubject(): Subject = createSubject(createSubjectContext())

    /**
     * Login based on a HTTP basic token in base64 format (e.g. zdzByZA==).
     */
    fun loginHttpBasic(token: String?): Subject {
        if (token == null) {
            log.debug("Token is null")
            return buildSubject()
        }
        val decodedToken = Base64.decodeToString(token).split(":")
        return try {
            login(buildSubject(), UsernamePasswordToken(decodedToken[0], decodedToken[1]))
        } catch (ex: Throwable) {
            log.info("Login failed: ${ex.message}")
            buildSubject()
        }
    }

    fun initFromConfig(config: Config) {
        val realm = getRealm()
        config.getObject("litfass.users").forEach { username, _ ->
            val password = config.getString("litfass.users.$username.password")
            val roles = config.getStringList("litfass.users.$username.roles").toSet()
            roles.forEach { role ->
                try {
                    Role.valueOf(role)
                } catch (ex: IllegalArgumentException) {
                    log.warn("Role $role is not supported on user $username")
                }
            }
            val account = SimpleAccount(username, password, realm.name, roles, null)
            realm.addAccount(account)
        }
    }

    fun getRealm(): SecurityRealm {
        return realms.first() as SecurityRealm
    }

}