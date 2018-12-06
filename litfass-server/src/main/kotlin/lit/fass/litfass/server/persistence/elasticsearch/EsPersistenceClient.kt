package lit.fass.litfass.server.persistence.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import lit.fass.litfass.server.persistence.PersistenceClient
import lit.fass.litfass.server.persistence.PersistenceException
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.RequestOptions.DEFAULT
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType.JSON
import org.slf4j.LoggerFactory

/**
 * @author Michael Mair
 */
class EsPersistenceClient(
    private val elasticsearchClient: RestHighLevelClient,
    private val jsonMapper: ObjectMapper
) : PersistenceClient {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun save(collection: String, data: Map<String, Any?>) {
        val indexRequest = IndexRequest(collection, "doc")
        indexRequest.source(jsonMapper.writeValueAsString(data), JSON)
        save(indexRequest)
    }

    override fun save(collection: String, id: Any?, data: Map<String, Any?>) {
        if (id !is String) {
            throw PersistenceException("id must be of type String")
        }

        log.debug("Indexing record with provided id $id")
        val indexRequest = IndexRequest(collection, "doc", id)
        indexRequest.source(jsonMapper.writeValueAsString(data), JSON)
        save(indexRequest)
    }

    private fun save(indexRequest: IndexRequest) {
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