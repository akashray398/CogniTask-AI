package com.example.knotes.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.R
import com.example.knotes.domain.model.Priority
import com.example.knotes.domain.model.Recurrence
import com.example.knotes.domain.model.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class EditTaskFragment : Fragment() {

    private val viewModel: TasksViewModel by viewModels()
    private val args: EditTaskFragmentArgs by navArgs()

    private var currentTask by mutableStateOf<Task?>(null)
    private var titleText by mutableStateOf("")
    private var tagsText by mutableStateOf("")
    private var selectedPriority by mutableStateOf(Priority.MEDIUM)
    private var selectedRecurrence by mutableStateOf(Recurrence.NONE)
    private var selectedDeadline by mutableLongStateOf(System.currentTimeMillis())
    private var selectedReminder by mutableStateOf<Long?>(null)
    private var selectedNoteId by mutableStateOf<Int?>(null)
    private var linkedNoteTitle by mutableStateOf<String?>(null)
    private var selectedRepeatEndDate by mutableStateOf<Long?>(null)

    @javax.inject.Inject
    lateinit var taskReminderManager: com.example.knotes.util.TaskReminderManager

    @javax.inject.Inject
    lateinit var getNotesUseCase: com.example.knotes.domain.usecase.GetNotesUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                EditTaskScreenCompose(
                    isEditing = currentTask != null,
                    title = titleText,
                    tags = tagsText,
                    priority = selectedPriority,
                    recurrence = selectedRecurrence,
                    selectedDeadline = selectedDeadline,
                    selectedReminder = selectedReminder,
                    selectedRepeatEndDate = selectedRepeatEndDate,
                    linkedNoteTitle = linkedNoteTitle,
                    onTitleChange = { titleText = it },
                    onTagsChange = { tagsText = it },
                    onPriorityChange = { selectedPriority = it },
                    onRecurrenceChange = { selectedRecurrence = it },
                    onPickDeadlineDateClick = { showDatePicker() },
                    onPickDeadlineTimeClick = { showTimePicker() },
                    onToggleReminder = { enabled ->
                        selectedReminder = if (enabled) selectedDeadline else null
                    },
                    onPickRepeatEndDateClick = { showRepeatEndDatePicker() },
                    onClearRepeatEndDateClick = { selectedRepeatEndDate = null },
                    onLinkNoteClick = { showNoteSelector() },
                    onSaveTaskClick = { saveTask() },
                    onDeleteTaskClick = { deleteTask() },
                    onNavigateBack = { findNavController().navigateUp() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = args.taskId
        if (taskId != -1) {
            loadTask(taskId)
        } else {
            selectedDeadline = getDefaultFutureDeadline()
            selectedReminder = selectedDeadline
        }
    }

    private fun getDefaultFutureDeadline(): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        if (currentHour >= 18) {
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 18)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun loadTask(taskId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                currentTask = viewModel.getTaskById(taskId)
                currentTask?.let {
                    titleText = it.title
                    tagsText = it.tags.joinToString(", ")
                    selectedPriority = it.priority
                    selectedRecurrence = it.recurrence
                    selectedDeadline = it.deadline ?: System.currentTimeMillis()
                    selectedReminder = it.reminderTime
                    selectedNoteId = it.relatedNoteId
                    selectedRepeatEndDate = it.recurrenceEndDate

                    if (selectedNoteId != null && selectedNoteId != -1) {
                        fetchNoteTitle(selectedNoteId!!)
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "Task not found", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchNoteTitle(noteId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = getNotesUseCase().first()
                val note = notes.find { it.id == noteId }
                linkedNoteTitle = note?.title ?: "Linked Note #$noteId"
            } catch (e: Exception) {
                linkedNoteTitle = "Linked Note #$noteId"
            }
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val initTime = selectedReminder ?: selectedDeadline
        calendar.timeInMillis = initTime

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select Reminder Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDeadline
            cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            cal.set(Calendar.MINUTE, timePicker.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            selectedDeadline = cal.timeInMillis
            selectedReminder = cal.timeInMillis
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Deadline Date")
            .setSelection(selectedDeadline)
            .build()

        datePicker.addOnPositiveButtonClickListener { pickerSelection ->
            val oldCal = Calendar.getInstance().apply { timeInMillis = selectedDeadline }
            val hour = oldCal.get(Calendar.HOUR_OF_DAY)
            val minute = oldCal.get(Calendar.MINUTE)

            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = pickerSelection
            }

            val newCal = Calendar.getInstance().apply {
                set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }

            selectedDeadline = newCal.timeInMillis

            if (selectedReminder != null) {
                val newRemCal = Calendar.getInstance().apply {
                    set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedReminder = newRemCal.timeInMillis
            }
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showNoteSelector() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notes = getNotesUseCase().first()
                val noteTitles = notes.map { it.title }.toTypedArray()

                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Note to Link")
                    .setItems(noteTitles) { _, which ->
                        val selectedNote = notes[which]
                        selectedNoteId = selectedNote.id
                        linkedNoteTitle = selectedNote.title
                    }
                    .setNeutralButton("Clear Link") { _, _ ->
                        selectedNoteId = null
                        linkedNoteTitle = null
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load notes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRepeatEndDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Repeat End Date")
            .setSelection(selectedRepeatEndDate ?: selectedDeadline)
            .build()

        datePicker.addOnPositiveButtonClickListener { pickerSelection ->
            selectedRepeatEndDate = pickerSelection
        }

        datePicker.show(parentFragmentManager, "REPEAT_END_DATE_PICKER")
    }

    private fun saveTask() {
        val title = titleText.trim()
        val tagsString = tagsText.trim()
        val tags = if (tagsString.isEmpty()) emptyList() else tagsString.split(",").map { it.trim() }

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Task title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val repeatEndDate = if (selectedRecurrence != Recurrence.NONE) selectedRepeatEndDate else null
        val effectiveReminder = selectedReminder ?: selectedDeadline

        val taskToSave = currentTask?.copy(
            title = title,
            deadline = selectedDeadline,
            priority = selectedPriority,
            recurrence = selectedRecurrence,
            recurrenceEndDate = repeatEndDate,
            tags = tags,
            reminderTime = effectiveReminder,
            relatedNoteId = selectedNoteId,
            updatedTime = System.currentTimeMillis()
        ) ?: Task(
            title = title,
            deadline = selectedDeadline,
            priority = selectedPriority,
            recurrence = selectedRecurrence,
            recurrenceEndDate = repeatEndDate,
            tags = tags,
            reminderTime = effectiveReminder,
            relatedNoteId = selectedNoteId,
            createdTime = System.currentTimeMillis(),
            updatedTime = System.currentTimeMillis()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentTask == null) {
                    val id = viewModel.insertTask(taskToSave)
                    taskReminderManager.scheduleTaskReminders(taskToSave.copy(id = id.toInt()))
                } else {
                    viewModel.updateTask(taskToSave)
                    taskReminderManager.cancelTaskReminders(taskToSave)
                    taskReminderManager.scheduleTaskReminders(taskToSave)
                }
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTask() {
        currentTask?.let {
            viewModel.deleteTask(it)
            findNavController().navigateUp()
        } ?: run {
            findNavController().navigateUp()
        }
    }
}
