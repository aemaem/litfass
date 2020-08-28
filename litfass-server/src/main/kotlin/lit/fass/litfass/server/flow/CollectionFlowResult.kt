package lit.fass.litfass.server.flow

/**
 * @author Michael Mair
 */
data class CollectionFlowResult(
    val insertOrUpdateData: Collection<Map<String, Any?>> = emptyList(),
    val deleteData: Collection<Map<String, Any?>> = emptyList()
)