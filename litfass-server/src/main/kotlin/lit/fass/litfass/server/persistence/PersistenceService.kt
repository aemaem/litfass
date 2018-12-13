package lit.fass.litfass.server.persistence

/**
 * @author Michael Mair
 */
interface PersistenceService {
    companion object {
        const val ID_KEY = "id"
    }

    fun isApplicable(datastore: Datastore): Boolean
    fun saveCollection(collection: String, data: Map<String, Any?>, id: Any? = null)
}