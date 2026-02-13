package com.titan.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.titan.reminder.data.AppDatabase
import com.titan.reminder.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Log.d(TAG, "Boot completed. Rescheduling all active alarms...")

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getInstance(context).reminderDao()
                    val activeReminders = dao.getAllActiveReminders()

                    Log.d(TAG, "Found ${activeReminders.size} active reminders to reschedule.")

                    for (reminder in activeReminders) {
                        // Only reschedule if the alarm time is in the future
                        if (reminder.timeInMillis > System.currentTimeMillis()) {
                            AlarmScheduler.schedule(
                                context = context,
                                reminderId = reminder.id,
                                title = reminder.title,
                                timeInMillis = reminder.timeInMillis
                            )
                            Log.d(TAG, "Rescheduled: id=${reminder.id}, title=${reminder.title}")
                        } else {
                            Log.d(TAG, "Skipped past alarm: id=${reminder.id}, title=${reminder.title}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
