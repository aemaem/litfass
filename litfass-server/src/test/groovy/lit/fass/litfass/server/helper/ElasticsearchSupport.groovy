package lit.fass.litfass.server.helper

import org.apache.http.HttpHost
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.builder.SearchSourceBuilder

import static org.elasticsearch.action.support.IndicesOptions.lenientExpandOpen
import static org.elasticsearch.client.RequestOptions.DEFAULT
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery

/**
 * @author Michael Mair
 */
trait ElasticsearchSupport {

    RestHighLevelClient elasticsearch = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))

    SearchResponse findAllIndex(String index) {
        def sourceBuilder = new SearchSourceBuilder()
        sourceBuilder.query(matchAllQuery())
        def request = new SearchRequest(index)
        request.source(sourceBuilder)
        return elasticsearch.search(request, DEFAULT)
    }

    void cleanDatabase() {
        DeleteIndexRequest request = new DeleteIndexRequest("_all")
        request.indicesOptions(lenientExpandOpen())
        elasticsearch.indices().delete(request, DEFAULT)
    }
}
