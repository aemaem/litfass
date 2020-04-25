package lit.fass.config

/**
 * Defines the Spring profiles available for the project.
 */
sealed class Profiles {
    companion object {
        const val NOT = "!"
        const val TEST = "TEST"
        const val SECURITY = "SECURITY"
        const val POSTGRES = "POSTGRES"
    }
}