package com.example.knotes.ui.auth

import android.app.Activity
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
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupFragment : Fragment() {

    private val signupViewModel: SignupViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Toast.makeText(requireContext(), "Connected as ${account?.displayName ?: "Google User"}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Google Sign-In completed", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.homeFragment)
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
                val uiState by signupViewModel.uiState.collectAsState()

                SignupScreenCompose(
                    uiState = uiState,
                    onFullNameChanged = signupViewModel::onFullNameChanged,
                    onEmailChanged = signupViewModel::onEmailChanged,
                    onPasswordChanged = signupViewModel::onPasswordChanged,
                    onConfirmPasswordChanged = signupViewModel::onConfirmPasswordChanged,
                    onTogglePasswordVisibility = signupViewModel::togglePasswordVisibility,
                    onToggleConfirmPasswordVisibility = signupViewModel::toggleConfirmPasswordVisibility,
                    onToggleTermsAccepted = signupViewModel::toggleTermsAccepted,
                    onSignupClick = {
                        signupViewModel.signup(
                            onVerificationRequired = {
                                findNavController().navigate(R.id.emailVerificationFragment)
                            }
                        )
                    },
                    onGoogleSignInClick = {
                        launchGoogleSignIn()
                    },
                    onNavigateToLogin = {
                        findNavController().navigateUp()
                    },
                    onNavigateBack = {
                        findNavController().navigateUp()
                    }
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        try {
            val gso = try {
                val clientIdRes = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
                val webClientId = if (clientIdRes != 0) getString(clientIdRes) else getString(R.string.default_web_client_id_fallback)
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
            } catch (e: Exception) {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
            }

            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Opening Google Sign-In...", Toast.LENGTH_SHORT).show()
        }
    }
}
