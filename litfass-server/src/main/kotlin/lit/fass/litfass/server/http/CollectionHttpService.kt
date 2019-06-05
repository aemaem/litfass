package lit.fass.litfass.server.http

import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*

/**
 * @author Michael Mair
 */
@Service
class CollectionHttpService(private val httpClient: WebClient) : HttpService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun get(url: String, username: String?, password: String?): Map<String, Any?> {
        log.info("Executing http get request on $url")
        return httpClient.get()
            .header(AUTHORIZATION, "Basic ${base64Encode("$username:$password")}")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, Any?>>() {})
            .switchIfEmpty(Mono.just(emptyMap()))
            .block()!!
    }

    private fun base64Encode(data: String): String {
        return Base64.getEncoder().encodeToString((data.toByteArray()))
    }
}