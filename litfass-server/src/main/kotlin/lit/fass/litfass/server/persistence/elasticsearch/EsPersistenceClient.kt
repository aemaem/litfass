package lit.fass.litfass.server.persistence.elasticsearch

import lit.fass.litfass.server.persistence.PersistenceClient
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType.JSON
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

/**
 * @author Michael Mair
 */
class EsPersistenceClient(uri: URI) : PersistenceClient {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    private var elasticsearchClient: RestHighLevelClient =
        RestHighLevelClient(RestClient.builder(HttpHost(uri.host, uri.port, uri.scheme)))

    override fun save(collection: String?, data: String?, metadata: Map<String, List<String>>) {
        val indexRequest = IndexRequest(collection, "doc")
        val meta = mutableListOf("\"timestamp\":\"${ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(UTC))}\"")
        meta.addAll(metadata.map {
            var value = ""
            if (it.value.size == 1) {
                value = it.value.first()
            } else if (it.value.size > 1) {
                value = it.value.joinToString()
            }
            "\"${it.key}\":\"${value}\""
        })

        indexRequest.source("""{${meta.joinToString()},"data":$data}""", JSON)
        elasticsearchClient.indexAsync(indexRequest, RequestOptions.DEFAULT, object : ActionListener<IndexResponse> {
            override fun onFailure(ex: Exception?) {
                log.error(ex?.message, ex)
            }

            override fun onResponse(response: IndexResponse?) {
                log.debug("Indexed record ${response?.id} for collection ${response?.index}")
            }
        })
    }
}