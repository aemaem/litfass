package lit.fass.litfass.server.config.yaml.model

import com.fasterxml.jackson.annotation.JsonCreator

/**
 * @author Michael Mair
 */
abstract class AbstractCollectionFlowStepConfig @JsonCreator constructor(open val description: String?) {
}