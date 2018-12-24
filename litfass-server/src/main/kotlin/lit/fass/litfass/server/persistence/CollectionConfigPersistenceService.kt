package lit.fass.litfass.server.persistence

/**
 * @author Michael Mair
 */
interface CollectionConfigPersistenceService {
    companion object {
        const val COLLECTION_CONFIG_TABLE = "collection_config"
    }

    fun saveConfig(collection: String, config: String)
    fun findConfig(collection: String): String?
    fun findConfigs(): List<String?>
    fun deleteConfig(collection: String)
}