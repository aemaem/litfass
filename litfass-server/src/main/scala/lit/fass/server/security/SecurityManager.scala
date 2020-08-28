package lit.fass.server.security

import java.util

import com.typesafe.config.Config
import lit.fass.server.security.SecurityManager.log
import org.apache.shiro.authc.{SimpleAccount, UsernamePasswordToken}
import org.apache.shiro.codec.Base64
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.subject.Subject
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._


/**
 * @author Michael Mair
 */
@Deprecated
class SecurityManager extends DefaultSecurityManager(new SecurityRealm()) {

  def buildSubject(): Subject = {
    createSubject(createSubjectContext())
  }

  /**
   * Login based on a HTTP basic token in base64 format (e.g. zdzByZA==).
   */
  def loginHttpBasic(token: String): Subject = {
    if (token == null) {
      log.debug("Token is null")
      return buildSubject()
    }
    val decodedToken = Base64.decodeToString(token).split(":")
    try {
      login(buildSubject(), new UsernamePasswordToken(decodedToken(0), decodedToken(1)))
    } catch {
      case ex: Throwable =>
        log.debug(s"Login failed: ${ex.getMessage}")
        buildSubject()
    }
  }

  def initFromConfig(config: Config): Unit = {
    val realm = getRealm
    config.getObject("litfass.users").forEach { (username, _) =>
      val password = config.getString(s"litfass.users.$username.password")
      val roles = config.getStringList(s"litfass.users.$username.roles")
        .toArray.toSet.asJava.asInstanceOf[util.Set[String]]
      val account = new SimpleAccount(username, password, realm.getName, roles, null)
      realm.addAccount(account)
    }
  }

  def getRealm: SecurityRealm = {
    getRealms.toArray.head.asInstanceOf[SecurityRealm]
  }
}

object SecurityManager {
  private val log = LoggerFactory.getLogger(classOf[SecurityManager])

  def apply(): SecurityManager = new SecurityManager()

  def apply(config: Config): SecurityManager = {
    val securityManager = new SecurityManager()
    securityManager.initFromConfig(config)
    securityManager
  }
}