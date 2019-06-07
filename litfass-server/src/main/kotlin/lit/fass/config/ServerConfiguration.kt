package lit.fass.config

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.litfass.server.ServerConfigurationAnchor
import lit.fass.litfass.server.persistence.JdbcDataSource
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthIndicatorAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient


@Configuration
@ComponentScan(basePackageClasses = [ServerConfigurationAnchor::class])
@EnableConfigurationProperties
@EnableAutoConfiguration(
    exclude = [
        ElasticsearchAutoConfiguration::class,
        ElasticsearchDataAutoConfiguration::class,
        ElasticSearchRestHealthIndicatorAutoConfiguration::class
    ]
)
@EnableWebFlux
class ServerConfiguration {

    @Bean
    fun jsonMapper() = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(WRITE_DATES_AS_TIMESTAMPS, false)
    }

    @Bean
    fun webClient() = WebClient.builder().build()

    //todo: make ds configurable
    @Bean
    fun jdbcDataSource() = JdbcDataSource("jdbc:postgresql://localhost:5432", "litfass", "admin", "admin", 1)

    //todo: create es client
    //@Bean
    //@Profile(ELASTICSEARCH)
    //fun elasticsearch() = RestHighLevelClient(RestClient.builder(*environment.config
    //    .property("litfass.elasticsearch.client.urls")
    //    .getString()
    //    .split(",")
    //    .map { URI(it) }
    //    .map { HttpHost(it.host, it.port, it.scheme) }.toTypedArray()
    //)
    //)
}
