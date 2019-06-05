package lit.fass.litfass.server

import lit.fass.config.ConfigurationAnchor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackageClasses = [ConfigurationAnchor::class])
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}

//todo: define api routes
//todo: define authentication