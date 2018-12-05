package lit.fass.litfass.server.persistence

/**
 * @author Michael Mair
 */
interface PersistenceClient {

    fun save(collection: String?, data: Map<String, Any?>)
}