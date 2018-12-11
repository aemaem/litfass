package lit.fass.litfass.server.execution

/**
 * @author Michael Mair
 */
interface ExecutionService {

    fun execute(collection: String, data: Map<String, Any?>)
}