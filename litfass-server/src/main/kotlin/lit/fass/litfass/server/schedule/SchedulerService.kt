package lit.fass.litfass.server.schedule

/**
 * @author Michael Mair
 */
interface SchedulerService {

    fun createJob(collection: String, cronExpression: String)
    fun cancelJob(collection: String)
}