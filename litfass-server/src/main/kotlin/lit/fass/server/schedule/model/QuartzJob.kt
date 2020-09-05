package lit.fass.server.schedule.model

/**
 * @author Michael Mair
 */
abstract class QuartzJob {

    abstract fun getType(): String
}