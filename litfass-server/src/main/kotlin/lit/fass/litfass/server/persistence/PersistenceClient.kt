package lit.fass.litfass.server.persistence

/**
 * @author Michael Mair
 */
interface PersistenceClient {
    companion object {
        const val ID_KEY = "id"
    }

    fun save(collection: String, data: Map<String, Any?>)
    fun save(collection: String, id: Any?, data: Map<String, Any?>)
}