package com.example.knotes.ui.notes

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.knotes.R
import com.example.knotes.databinding.FragmentNoteDetailBinding
import com.example.knotes.domain.model.Note
import com.example.knotes.domain.model.Priority
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private val args: NoteDetailFragmentArgs by navArgs()
    private var currentNote: Note? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdgeInsets()
        setupToolbar()
        loadNote()

        binding.fabEdit.setOnClickListener {
            currentNote?.let { note ->
                val action = NoteDetailFragmentDirections.actionNoteDetailFragmentToEditNoteFragment(note.id)
                findNavController().navigate(action)
            }
        }
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.setPadding(0, statusBarInsets.top, 0, 0)

            val fabParams = binding.fabEdit.layoutParams as? ViewGroup.MarginLayoutParams
            if (fabParams != null) {
                val baseMargin = (20 * resources.displayMetrics.density).toInt()
                fabParams.bottomMargin = navBarInsets.bottom + baseMargin
                binding.fabEdit.layoutParams = fabParams
            }

            binding.contentContainer.setPadding(
                binding.contentContainer.paddingLeft,
                binding.contentContainer.paddingTop,
                binding.contentContainer.paddingRight,
                (88 * resources.displayMetrics.density).toInt() + navBarInsets.bottom
            )

            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            val note = currentNote ?: return@setOnMenuItemClickListener false
            when (menuItem.itemId) {
                R.id.action_pin -> {
                    viewModel.togglePin(note)
                    val updatedNote = note.copy(isPinned = !note.isPinned)
                    currentNote = updatedNote
                    updateMenuIcons(updatedNote)
                    val msg = if (updatedNote.isPinned) "Note pinned" else "Note unpinned"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_favorite -> {
                    viewModel.toggleFavorite(note)
                    val updatedNote = note.copy(isFavorite = !note.isFavorite)
                    currentNote = updatedNote
                    updateMenuIcons(updatedNote)
                    val msg = if (updatedNote.isFavorite) "Added to favorites" else "Removed from favorites"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_share -> {
                    shareNote(note)
                    true
                }
                R.id.action_history -> {
                    val action = NoteDetailFragmentDirections.actionNoteDetailFragmentToEditNoteFragment(note.id)
                    findNavController().navigate(action)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            val note = viewModel.getNoteById(args.noteId)
            if (note != null) {
                currentNote = note
                displayNote(note)
                updateMenuIcons(note)
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun updateMenuIcons(note: Note) {
        val menu = binding.toolbar.menu
        val pinItem = menu.findItem(R.id.action_pin)
        val favItem = menu.findItem(R.id.action_favorite)

        if (pinItem != null) {
            pinItem.title = if (note.isPinned) getString(R.string.action_unpin) else getString(R.string.action_pin)
            pinItem.icon?.setTint(
                if (note.isPinned) ContextCompat.getColor(requireContext(), R.color.electric_blue)
                else ContextCompat.getColor(requireContext(), R.color.outline)
            )
        }

        if (favItem != null) {
            favItem.title = if (note.isFavorite) getString(R.string.action_unfavorite) else getString(R.string.action_favorite)
            favItem.icon?.setTint(
                if (note.isFavorite) ContextCompat.getColor(requireContext(), R.color.error)
                else ContextCompat.getColor(requireContext(), R.color.outline)
            )
        }
    }

    private fun displayNote(note: Note) {
        binding.tvTitle.text = note.title.ifBlank { getString(R.string.untitled_note) }

        if (note.content.contains("<") && note.content.contains(">")) {
            binding.tvContent.text = Html.fromHtml(note.content, Html.FROM_HTML_MODE_COMPACT)
        } else {
            binding.tvContent.text = note.content
        }

        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val lastUpdated = if (note.updatedTime > 0) note.updatedTime else note.createdTime
        binding.tvTimestamp.text = getString(R.string.updated_at, sdf.format(Date(lastUpdated)))

        // Priority Badge
        if (note.priority != Priority.LOW) {
            binding.tvPriority.visibility = View.VISIBLE
            binding.tvPriority.text = note.priority.name
            val badgeBg = when (note.priority) {
                Priority.HIGH -> R.color.errorContainer
                Priority.MEDIUM -> R.color.secondaryContainer
                else -> R.color.surfaceVariant
            }
            val textColor = when (note.priority) {
                Priority.HIGH -> R.color.onErrorContainer
                Priority.MEDIUM -> R.color.onSecondaryContainer
                else -> R.color.onSurfaceVariant
            }
            binding.tvPriority.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_linked_note)
            binding.tvPriority.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), badgeBg))
            binding.tvPriority.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        } else {
            binding.tvPriority.visibility = View.GONE
        }

        // Reminder Time
        if (note.reminderTime != null && note.reminderTime > 0) {
            binding.cardReminder.visibility = View.VISIBLE
            binding.tvReminderTime.text = sdf.format(Date(note.reminderTime))
        } else {
            binding.cardReminder.visibility = View.GONE
        }

        // Tags
        binding.chipGroupTags.removeAllViews()
        if (note.tags.isNotEmpty()) {
            binding.chipGroupTags.visibility = View.VISIBLE
            note.tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = getString(R.string.tag_format, tag)
                    isClickable = false
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.surfaceVariant)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariant))
                    setEnsureMinTouchTargetSize(false)
                }
                binding.chipGroupTags.addView(chip)
            }
        } else {
            binding.chipGroupTags.visibility = View.GONE
        }
    }

    private fun shareNote(note: Note) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Note"))
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                currentNote?.let {
                    viewModel.deleteNote(it)
                    findNavController().navigateUp()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}