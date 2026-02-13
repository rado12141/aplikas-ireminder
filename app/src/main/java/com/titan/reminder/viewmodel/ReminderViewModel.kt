package com.titan.reminder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.titan.reminder.data.AppDatabase
import com.titan.reminder.data.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).reminderDao()

    val allReminders: LiveData<List<ReminderEntity>> = dao.getAllReminders()

    fun insertReminder(reminder: ReminderEntity, onInserted: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.insert(reminder)
            onInserted(id)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(reminder)
        }
    }
}
