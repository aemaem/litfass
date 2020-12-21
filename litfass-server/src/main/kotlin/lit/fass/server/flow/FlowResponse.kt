package lit.fass.server.flow

import lit.fass.server.flow.FlowAction.ADD

/**
 * @author Michael Mair
 */
data class FlowResponse(
    val data: Collection<Map<String, Any?>>,
    val action: FlowAction = ADD
)
