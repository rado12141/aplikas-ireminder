package com.titan.reminder.ui

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.titan.reminder.data.ReminderEntity
import com.titan.reminder.databinding.ActivityMainBinding
import com.titan.reminder.util.AlarmScheduler
import com.titan.reminder.viewmodel.ReminderViewModel
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ReminderViewModel
    private lateinit var adapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ReminderViewModel::class.java]

        setupRecyclerView()
        setupTimePicker()
        setupSetAlarmButton()
        observeReminders()
        checkExactAlarmPermission()
    }

    private fun setupRecyclerView() {
        adapter = ReminderAdapter { reminder ->
            // On delete click
            AlarmScheduler.cancel(this, reminder.id)
            viewModel.deleteReminder(reminder)
            Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewReminders.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupTimePicker() {
        binding.timePicker.setIs24HourView(true)
    }

    private fun setupSetAlarmButton() {
        binding.btnSetAlarm.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()

            if (title.isEmpty()) {
                binding.etTitle.error = "Please enter a title"
                binding.etTitle.requestFocus()
                return@setOnClickListener
            }

            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the selected time is in the past, set it for the next day
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val timeInMillis = calendar.timeInMillis

            val reminder = ReminderEntity(
                title = title,
                timeInMillis = timeInMillis,
                active = true
            )

            viewModel.insertReminder(reminder) { insertedId ->
                runOnUiThread {
                    AlarmScheduler.schedule(
                        context = this,
                        reminderId = insertedId.toInt(),
                        title = title,
                        timeInMillis = timeInMillis
                    )

                    binding.etTitle.text?.clear()

                    val formattedTime = String.format("%02d:%02d", hour, minute)
                    Toast.makeText(
                        this,
                        "Alarm set for $formattedTime",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeReminders() {
        viewModel.allReminders.observe(this) { reminders ->
            adapter.submitList(reminders)

            if (reminders.isEmpty()) {
                binding.tvEmptyState.visibility = android.view.View.VISIBLE
                binding.recyclerViewReminders.visibility = android.view.View.GONE
            } else {
                binding.tvEmptyState.visibility = android.view.View.GONE
                binding.recyclerViewReminders.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
