package lit.fass.litfass.server

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.apache.http.HttpHost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class AppTest {

    @Test
    fun pathRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(Get, "/").apply {
                assertEquals(OK, response.status())
                assertEquals(
                    """{
  "application" : "LITFASS",
  "description" : "Lightweight Integrated Tailorable Flow Aware Software Service"
}""", response.content
                )
            }
        }
    }

    @Test
    fun pathCollections() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(Post, "/collections/foo?param1=true&param1=1") {
                setBody("{\"test\":1}")
            }.apply {
                assertEquals(OK, response.status())
            }
        }

        val esClient = RestHighLevelClient(RestClient.builder(HttpHost("localhost", 9200, "http")))
        val searchRequest = SearchRequest("foo")
        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder.query(QueryBuilders.matchAllQuery())
        searchRequest.source(searchSourceBuilder)
        await until { esClient.search(searchRequest, RequestOptions.DEFAULT).hits.totalHits == 1L }
    }
}
