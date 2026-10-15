package com.example.knotes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.example.knotes.data.repository.SyncRepository
import com.example.knotes.domain.usecase.ClearAllDataUseCase
import com.example.knotes.domain.usecase.ExportDataUseCase
import com.example.knotes.domain.usecase.ImportDataUseCase
import com.example.knotes.util.PreferenceManager
import com.example.knotes.util.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val authViewModel: com.example.knotes.ui.auth.AuthViewModel by viewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var exportDataUseCase: ExportDataUseCase

    @Inject
    lateinit var importDataUseCase: ImportDataUseCase

    @Inject
    lateinit var clearAllDataUseCase: ClearAllDataUseCase

    @Inject
    @JvmField
    var auth: FirebaseAuth? = null

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val outputStream = requireContext().contentResolver.openOutputStream(it)
                    if (outputStream != null) {
                        exportDataUseCase(outputStream).onSuccess {
                            Toast.makeText(requireContext(), "Data exported successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error opening output stream", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        importDataUseCase(inputStream).onSuccess {
                            Toast.makeText(requireContext(), "Data imported successfully", Toast.LENGTH_SHORT).show()
                        }.onFailure { e ->
                            Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error opening input stream", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by settingsManager.themeMode.collectAsState(initial = 0)
                val isGridView by settingsManager.isGridView.collectAsState(initial = true)
                val isAiEnabled by settingsManager.isAiEnabled.collectAsState(initial = false)
                val syncTimestamp by settingsManager.lastSynced.collectAsState(initial = 0L)
                val syncState by syncRepository.syncState.collectAsState(initial = SyncRepository.SyncState.Idle)
                val user = auth?.currentUser
                val profile = user?.let {
                    com.example.knotes.domain.repository.UserProfile(
                        uid = it.uid,
                        email = it.email ?: "",
                        displayName = it.displayName ?: "KNotes User",
                        isEmailVerified = true
                    )
                }

                SettingsScreenCompose(
                    currentUser = profile,
                    themeMode = themeMode,
                    isGridView = isGridView,
                    isAiEnabled = isAiEnabled,
                    syncTimestamp = syncTimestamp,
                    isSyncing = syncState is SyncRepository.SyncState.Syncing,
                    isAppLocked = preferenceManager.isAppLocked(),
                    onNavigateBack = {
                        findNavController().navigateUp()
                    },
                    onLoginClick = {
                        findNavController().navigate(R.id.loginFragment)
                    },
                    onSignupClick = {
                        findNavController().navigate(R.id.signupFragment)
                    },
                    onLogoutClick = {
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Log Out of KNotes?")
                            .setMessage("Are you sure you want to log out? Your notes and tasks will remain safely stored on your account.")
                            .setPositiveButton("Log Out") { _, _ ->
                                authViewModel.logout {
                                    findNavController().popBackStack(R.id.homeFragment, false)
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    },
                    onSyncClick = {
                        syncRepository.startSync(manual = true)
                    },
                    onSetThemeMode = { mode ->
                        lifecycleScope.launch {
                            settingsManager.setThemeMode(mode)
                        }
                    },
                    onSetGridView = { isGrid ->
                        lifecycleScope.launch {
                            settingsManager.setGridView(isGrid)
                        }
                    },
                    onToggleAiEnabled = { enabled ->
                        lifecycleScope.launch {
                            settingsManager.setAiEnabled(enabled)
                        }
                    },
                    onResetAiConsent = {
                        lifecycleScope.launch {
                            settingsManager.setAiConsentGiven(false)
                            settingsManager.setAiEnabled(false)
                            Toast.makeText(requireContext(), "AI settings and consent reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleAppLock = { locked ->
                        preferenceManager.setAppLocked(locked)
                    },
                    onExportData = {
                        exportLauncher.launch("knotes_backup_${System.currentTimeMillis()}.json")
                    },
                    onImportData = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    onClearTrash = {
                        lifecycleScope.launch {
                            syncRepository.clearTrash()
                            Toast.makeText(requireContext(), "Trash cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearAllData = {
                        lifecycleScope.launch {
                            clearAllDataUseCase().onSuccess {
                                Toast.makeText(requireContext(), "All local data deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
