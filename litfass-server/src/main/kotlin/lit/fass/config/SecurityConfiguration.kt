package lit.fass.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
@EnableWebFluxSecurity
class SecurityConfiguration {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/actuator/health", "/collections/*").permitAll()
            .anyExchange().authenticated()
            .and()
            .httpBasic()
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun userDetailsService(securityProperties: SecurityProperties): MapReactiveUserDetailsService {
        val users = mutableListOf<UserDetails>()
        securityProperties.users.forEach { (name, password) ->
            log.debug("Adding user $name")
            users.add(
                User.withUsername(name)
                    .password(passwordEncoder().encode(password))
                    .roles("ADMIN")
                    .build()
            )
        }
        return MapReactiveUserDetailsService(users)
    }
}
