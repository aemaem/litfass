package lit.fass.server.security

import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.subject.support.DefaultSubjectContext
import org.apache.shiro.subject.{SimplePrincipalCollection, Subject}

/**
 * @author Michael Mair
 */
class SafeguardManager extends DefaultSecurityManager(new ConfigRealm()) {

  def getSubject(id: String): Subject = {
    val context = new DefaultSubjectContext {
      setAuthenticated(true)
      setPrincipals(new SimplePrincipalCollection(id, "basic"))
    }
    createSubject(context)
  }
}
