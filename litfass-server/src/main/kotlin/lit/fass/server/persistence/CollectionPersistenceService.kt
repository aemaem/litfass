package lit.fass.server.persistence

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
    fun saveCollection(collection: String, data: Collection<Map<String, Any?>>)
    fun removeCollection(collection: String, ids: Collection<String>)
    fun removeCollection(collection: String, id: String)
    fun findCollectionData(collection: String, id: String): Map<String, Any?>
    fun deleteBefore(collection: String, timestamp: OffsetDateTime)
}