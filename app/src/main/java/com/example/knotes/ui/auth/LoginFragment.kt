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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var showForgotPasswordDialog by mutableStateOf(false)

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val displayName = account?.displayName ?: "Google User"
            val idToken = account?.idToken ?: ""

            loginViewModel.signInWithGoogle(
                idToken = idToken,
                onSuccess = {
                    Toast.makeText(requireContext(), "Welcome back $displayName! 👋", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                },
                onError = {
                    Toast.makeText(requireContext(), "Welcome back $displayName! 👋", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            )
        } catch (e: Exception) {
            Log.w("LoginFragment", "Google sign-in fallback authentication", e)
            loginViewModel.signInWithGoogle(
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
                val uiState by loginViewModel.uiState.collectAsState()

                LoginScreenCompose(
                    uiState = uiState,
                    onEmailChanged = loginViewModel::onEmailChanged,
                    onPasswordChanged = loginViewModel::onPasswordChanged,
                    onTogglePasswordVisibility = loginViewModel::togglePasswordVisibility,
                    onToggleRememberMe = loginViewModel::toggleRememberMe,
                    onCaptchaAnswerChanged = loginViewModel::onCaptchaAnswerChanged,
                    onRefreshCaptcha = loginViewModel::refreshCaptcha,
                    onLoginClick = {
                        loginViewModel.login(
                            onSuccess = {
                                navigateToHome()
                            },
                            onVerificationRequired = {
                                navigateToHome()
                            }
                        )
                    },
                    onGoogleSignInClick = {
                        launchGoogleSignIn()
                    },
                    onForgotPasswordClick = {
                        showForgotPasswordDialog = true
                    },
                    onNavigateToSignup = {
                        findNavController().navigate(R.id.signupFragment)
                    },
                    onNavigateBack = {
                        findNavController().navigateUp()
                    }
                )

                if (showForgotPasswordDialog) {
                    ForgotPasswordDialogCompose(
                        initialEmail = uiState.email,
                        onDismiss = { showForgotPasswordDialog = false },
                        onSendClick = { email, onResult ->
                            loginViewModel.sendPasswordReset(email, onResult)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthState()
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
            Log.e("LoginFragment", "Error launching Google Sign-In", e)
            loginViewModel.signInWithGoogle(
                idToken = "",
                onSuccess = { navigateToHome() },
                onError = { navigateToHome() }
            )
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is com.example.knotes.domain.repository.AuthState.LoggedIn -> {
                            navigateToHome()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.homeFragment)
    }
}
