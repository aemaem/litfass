package lit.fass.litfass.server.persistence

import java.time.OffsetDateTime

/**
 * @author Michael Mair
 */
interface CollectionPersistenceService {
    companion object {
        const val ID_KEY = "id"
    }

    fun isApplicable(datastore: Datastore): Boolean
    fun saveCollection(collection: String, data: Map<String, Any?>, id: Any? = null)
    fun findCollectionData(collection: String, id: String): Map<String, Any?>
    fun deleteBefore(collection: String, timestamp: OffsetDateTime)
}