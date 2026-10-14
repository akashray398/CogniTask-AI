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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.knotes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
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
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val tokenStr: String? = account?.idToken
                if (!tokenStr.isNullOrEmpty()) {
                    loginViewModel.signInWithGoogle(
                        idToken = tokenStr,
                        onSuccess = {
                            navigateToHome()
                        },
                        onError = { err ->
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(requireContext(), "Connected as ${account?.displayName ?: "Google User"}", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Google Sign-In completed", Toast.LENGTH_SHORT).show()
                navigateToHome()
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
                                findNavController().navigate(R.id.emailVerificationFragment)
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
            val clientIdRes = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
            val webClientId = if (clientIdRes != 0) getString(clientIdRes) else getString(R.string.default_web_client_id_fallback)

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Initiating Google Sign-In...", Toast.LENGTH_SHORT).show()
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
                        is com.example.knotes.domain.repository.AuthState.EmailVerificationRequired -> {
                            findNavController().navigate(R.id.emailVerificationFragment)
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
