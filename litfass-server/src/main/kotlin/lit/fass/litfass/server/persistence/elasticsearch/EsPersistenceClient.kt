package lit.fass.litfass.server.persistence.elasticsearch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.litfass.server.persistence.PersistenceClient
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RequestOptions.DEFAULT
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType.JSON
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * @author Michael Mair
 */
class EsPersistenceClient(uris: List<URI>) : PersistenceClient {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private val jsonMapper = jacksonObjectMapper()
    private var elasticsearchClient: RestHighLevelClient =
        RestHighLevelClient(RestClient.builder(*uris.map { HttpHost(it.host, it.port, it.scheme) }.toTypedArray()))

    override fun save(collection: String?, data: Map<String, Any?>) {
        val indexRequest = IndexRequest(collection, "doc")
        indexRequest.source(jsonMapper.writeValueAsString(data), JSON)
        elasticsearchClient.indexAsync(indexRequest, DEFAULT, object : ActionListener<IndexResponse> {
            override fun onFailure(ex: Exception?) {
                log.error(ex?.message, ex)
            }

            override fun onResponse(response: IndexResponse?) {
                log.debug("Indexed record ${response?.id} for collection ${response?.index}")
            }
        })
    }
}