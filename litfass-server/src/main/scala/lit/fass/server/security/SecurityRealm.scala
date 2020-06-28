package lit.fass.server.security

import org.apache.shiro.authc.SimpleAccount
import org.apache.shiro.realm.SimpleAccountRealm

/**
 * @author Michael Mair
 */
class SecurityRealm extends SimpleAccountRealm {

  def addAccount(account: SimpleAccount): Unit = {
    add(account)
  }
}
