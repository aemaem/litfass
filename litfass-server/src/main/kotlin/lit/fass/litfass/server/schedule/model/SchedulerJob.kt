package lit.fass.litfass.server.schedule.model

import com.cronutils.model.Cron
import kotlinx.coroutines.Job

/**
 * @author Michael Mair
 */
data class SchedulerJob(val cron: Cron, val job: Job)