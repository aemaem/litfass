package lit.fass.server.security

import org.apache.shiro.authc.{AuthenticationInfo, AuthenticationToken}
import org.apache.shiro.authz.{AuthorizationInfo, SimpleAuthorizationInfo}
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

/**
 * @author Michael Mair
 */
// todo: get information from configuration
// todo: support authentication
class ConfigRealm extends AuthorizingRealm {

  /**
   * This realm does not implement authentication.
   *
   * @return false
   */
  override def supports(token: AuthenticationToken): Boolean = false

  override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
    val subjectId = getAvailablePrincipal(principals).toString

    if (subjectId == "Sepp") {
      return new SimpleAuthorizationInfo {
        addStringPermission("greet")
      }
    }
    new SimpleAuthorizationInfo()
  }

  //noinspection NotImplementedCode
  override def doGetAuthenticationInfo(token: AuthenticationToken): AuthenticationInfo = ???
}
