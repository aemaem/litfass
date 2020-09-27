package lit.fass.server.http

import akka.http.javadsl.model.StatusCodes.FORBIDDEN
import akka.http.javadsl.model.StatusCodes.UNAUTHORIZED
import akka.http.javadsl.model.headers.Authorization
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.Route
import lit.fass.server.logger
import lit.fass.server.security.Role
import lit.fass.server.security.SecurityManager
import org.apache.shiro.subject.Subject

/**
 * @author Michael Mair
 */
abstract class SecurityDirectives(private val securityManager: SecurityManager) : AllDirectives() {

    companion object {
        private val log = this.logger()
    }

    /**
     * Authenticates the given credentials in the Authorization header and provides the authenticated subject.
     */
    protected fun authenticate(inner: (subject: Subject) -> Route): Route {
        return headerValueByType(Authorization::class.java) { header ->
            val basicCredentials = header.value().split(" ")[1]
            val subject = securityManager.loginHttpBasic(basicCredentials)
            if (subject.isAuthenticated) {
                return@headerValueByType inner.invoke(subject)
            }
            complete(UNAUTHORIZED)
        }
    }

    /**
     * Authorizes the given subject for the permitted role.
     */
    protected fun authorize(subject: Subject, permittedRole: Role, inner: () -> Route): Route {
        return authorize(subject, listOf(permittedRole), inner)
    }

    /**
     * Authorizes the given subject for one of the permitted roles.
     */
    protected fun authorize(subject: Subject, permittedRoles: List<Role>, inner: () -> Route): Route {
        log.debug("Checking authorization for subject ${subject.principal} for roles $permittedRoles")
        if (permittedRoles.any { subject.hasRole(it.name) }) {
            return inner.invoke()
        }
        return complete(FORBIDDEN)
    }

}