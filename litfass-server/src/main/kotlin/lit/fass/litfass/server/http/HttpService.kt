package lit.fass.litfass.server.http

/**
 * @author Michael Mair
 */
interface HttpService {
    fun get(url: String, username: String?, password: String?): Map<String, Any?>
}