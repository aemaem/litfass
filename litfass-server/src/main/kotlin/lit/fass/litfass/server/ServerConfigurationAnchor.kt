package lit.fass.litfass.server

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Target(VALUE_PARAMETER)
@Retention(SOURCE)
annotation class ServerConfigurationAnchor