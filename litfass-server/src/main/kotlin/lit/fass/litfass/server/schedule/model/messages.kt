package lit.fass.litfass.server.schedule.model

import com.cronutils.model.Cron

/**
 * @author Michael Mair
 */
sealed class ScheduledJobMessage

data class CreateJobMessage(val name: String, val cron: Cron) : ScheduledJobMessage()
data class CancelJobMessage(val name: String) : ScheduledJobMessage()
