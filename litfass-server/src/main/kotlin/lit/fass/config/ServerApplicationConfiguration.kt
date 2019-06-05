package lit.fass.config

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.litfass.server.ServerConfigurationAnchor
import lit.fass.litfass.server.script.groovy.GroovyScriptEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackageClasses = [ServerConfigurationAnchor::class])
class ServerApplicationConfiguration {

    @Bean
    fun jsonMapper() = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(WRITE_DATES_AS_TIMESTAMPS, false)
    }

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
