package lit.fass.server.security

import org.apache.shiro.authc.SimpleAccount
import org.apache.shiro.realm.SimpleAccountRealm

/**
 * @author Michael Mair
 */
@Deprecated
class SecurityRealm extends SimpleAccountRealm {

  def addAccount(account: SimpleAccount): Unit = {
    add(account)
  }
}
