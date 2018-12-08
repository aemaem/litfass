package lit.fass.litfass.server.http

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.Authorization
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Michael Mair
 */
class CollectionHttpService(private val httpClient: HttpClient) : HttpService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun get(url: String, username: String?, password: String?): Map<String, Any?> = runBlocking {
        log.info("Executing http get request on $url")
        httpClient.get<Map<String, Any?>>(url) {
            if (!username.isNullOrEmpty()) {
                header(Authorization, "Basic ${base64Encode("$username:$password")}")
            }
        }
    }

    private fun base64Encode(data: String): String {
        return Base64.getEncoder().encodeToString((data.toByteArray()))
    }
}