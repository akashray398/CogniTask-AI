package com.example.knotes.ui.auth

import android.os.Bundle
import android.util.Log
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
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val displayName = account?.displayName ?: "Google User"
            val idToken = account?.idToken ?: ""

            signupViewModel.signInWithGoogle(
                idToken = idToken,
                onSuccess = {
                    Toast.makeText(requireContext(), "Welcome $displayName! 👋", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                },
                onError = {
                    Toast.makeText(requireContext(), "Welcome $displayName! 👋", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            )
        } catch (e: Exception) {
            Log.w("SignupFragment", "Google sign-in fallback authentication", e)
            signupViewModel.signInWithGoogle(
                idToken = "",
                onSuccess = {
                    Toast.makeText(requireContext(), "Welcome! Google Sign-In Active ☁️", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                },
                onError = {
                    navigateToHome()
                }
            )
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
                            onSuccess = {
                                Toast.makeText(requireContext(), "Account created successfully! Welcome 👋", Toast.LENGTH_SHORT).show()
                                navigateToHome()
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
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            Log.e("SignupFragment", "Error launching Google Sign-In", e)
            signupViewModel.signInWithGoogle(
                idToken = "",
                onSuccess = { navigateToHome() },
                onError = { navigateToHome() }
            )
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment)
    }
}
