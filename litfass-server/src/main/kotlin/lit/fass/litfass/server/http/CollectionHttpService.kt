package lit.fass.litfass.server.http

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.apache.http.HttpHeaders.ACCEPT
import org.apache.http.HttpHeaders.AUTHORIZATION
import org.apache.http.client.config.CookieSpecs.DEFAULT
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder.create
import org.apache.http.util.EntityUtils.toByteArray
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE
import org.springframework.stereotype.Service
import java.util.*

/**
 * @author Michael Mair
 */
@Service
class CollectionHttpService(private val jsonMapper: ObjectMapper) : HttpService {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun get(
        url: String,
        headers: Map<String, String?>,
        username: String?,
        password: String?
    ): Map<String, Any?> {
        log.info("Executing http get request on $url")
        val request = HttpGet(url)
        request.addHeader(ACCEPT, APPLICATION_JSON_UTF8_VALUE)
        if (!username.isNullOrBlank()) {
            request.addHeader(AUTHORIZATION, "Basic ${base64Encode("$username:$password")}")
        }
        headers.entries.forEach { header ->
            log.debug("Adding header with name ${header.key} and value ${header.value}")
            request.addHeader(header.key, header.value)
        }

        var response: CloseableHttpResponse? = null
        try {
            response = create()
                .setDefaultRequestConfig(
                    RequestConfig.custom().setCookieSpec(DEFAULT).build()
                )
                .build().execute(request)
            val data = parse(toByteArray(response.entity))
            return data + mapOf("requestHeaders" to headers.entries.map { it.key to it.value }.toMap())
        } catch (ex: Exception) {
            log.error("Exception occurred for HTTP request $url: ${ex.message}", ex)
            throw ex
        } finally {
            response?.close()
        }
    }

    private fun parse(data: ByteArray): Map<String, Any?> {
        try {
            return jsonMapper.readValue(data, object : TypeReference<Map<String, Any?>>() {})
        } catch (ex: MismatchedInputException) {
            log.debug("Unable to parse to map; trying to parse to list")
            if (log.isDebugEnabled) ex.printStackTrace()
            try {
                val parsedValue = jsonMapper.readValue<List<Map<String, Any?>>>(
                    data,
                    object : TypeReference<List<Map<String, Any?>>?>() {}
                )
                return mapOf("http" to parsedValue)
            } catch (e: Exception) {
                log.error("Unable to parse response: ${e.message}", e)
                throw e
            }
        }
    }

    private fun base64Encode(data: String): String {
        return Base64.getEncoder().encodeToString((data.toByteArray()))
    }
}