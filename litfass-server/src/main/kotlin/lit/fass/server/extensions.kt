package lit.fass.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory


fun Any.logger(): Logger {
    if (this::class.isCompanion) {
        return LoggerFactory.getLogger(this::class.java.enclosingClass)
    }
    return LoggerFactory.getLogger(this::class.java)
}

fun LitfassApplication.logger(): Logger {
    return LoggerFactory.getLogger(this::class.java)
}