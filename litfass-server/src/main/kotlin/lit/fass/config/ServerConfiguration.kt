package lit.fass.config

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import lit.fass.litfass.server.ServerConfigurationAnchor
import lit.fass.litfass.server.persistence.JdbcDataSource
import lit.fass.litfass.server.persistence.JdbcProperties
import lit.fass.litfass.server.rest.CollectionsHandler
import lit.fass.litfass.server.rest.ConfigsHandler
import lit.fass.litfass.server.rest.ScriptHandler
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse


@Configuration
@ComponentScan(basePackageClasses = [ServerConfigurationAnchor::class])
@EnableConfigurationProperties
@EnableAutoConfiguration
@EnableWebFlux
class ServerConfiguration {

    @Bean
    fun jsonMapper() = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(WRITE_DATES_AS_TIMESTAMPS, false)
    }

    @Bean
    fun webClient() = WebClient.create()

    @Bean
    fun jdbcDataSource(jdbcProperties: JdbcProperties) = JdbcDataSource(
        jdbcProperties.url,
        jdbcProperties.database,
        jdbcProperties.username,
        jdbcProperties.password,
        jdbcProperties.poolSize
    )

    @Bean
    fun collectionsRouter(handler: CollectionsHandler): RouterFunction<ServerResponse> = route()
        .POST("/collections/{collection}", accept(APPLICATION_JSON), HandlerFunction(handler::addCollection))
        .GET("/collections/{collection}", HandlerFunction(handler::addCollection))
        .GET("/collections/{collection}/{id}", HandlerFunction(handler::getCollection))
        .build()

    @Bean
    fun configsRouter(handler: ConfigsHandler): RouterFunction<ServerResponse> = route()
        .POST("/configs", accept(TEXT_PLAIN), HandlerFunction(handler::addConfig))
        .GET("/configs", HandlerFunction(handler::getConfigs))
        .GET("/configs/{collection}", HandlerFunction(handler::getConfig))
        .DELETE("/configs/{collection}", HandlerFunction(handler::deleteConfig))
        .build()

    @Bean
    fun scriptsRouter(handler: ScriptHandler): RouterFunction<ServerResponse> = route()
        .POST("/script/{language}/test", accept(APPLICATION_JSON), HandlerFunction(handler::testScript))
        .build()
}
