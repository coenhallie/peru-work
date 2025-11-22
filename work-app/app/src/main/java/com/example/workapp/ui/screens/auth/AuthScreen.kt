package com.example.workapp.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.workapp.data.model.UserRole
import com.example.workapp.ui.components.AddressAutofillTextField
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.viewmodel.AuthState
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * Modern authentication screen with Google Sign-In as primary option
 * and email/password as alternative
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    Log.d("AuthScreen", "Composing AuthScreen with state: $authState")
    val snackbarHostState = remember { SnackbarHostState() }
    var showEmailAuth by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var googleUser by remember { mutableStateOf<FirebaseUser?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Credential Manager
    val credentialManager = remember { CredentialManager.create(context) }
    
    // Handle Google Sign-In with Credential Manager
    val handleGoogleSignIn: () -> Unit = {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(com.example.workapp.R.string.default_web_client_id))
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                
                val credential = result.credential
                
                when (credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                Log.d("AuthScreen", "Got Google ID token, signing in...")
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                            } catch (e: Exception) {
                                Log.e("AuthScreen", "Error creating GoogleIdTokenCredential", e)
                                snackbarHostState.showSnackbar("Failed to process Google credential: ${e.message}")
                            }
                        } else {
                            Log.e("AuthScreen", "Unexpected credential type: ${credential.type}")
                            snackbarHostState.showSnackbar("Unexpected credential type")
                        }
                    }
                    else -> {
                        Log.e("AuthScreen", "Unexpected credential class: ${credential::class.java.name}")
                        snackbarHostState.showSnackbar("Unexpected credential format")
                    }
                }
            } catch (e: GetCredentialException) {
                // Handle credential retrieval error
                Log.e("AuthScreen", "GetCredentialException", e)
                snackbarHostState.showSnackbar(
                    message = "Google Sign-In cancelled or failed: ${e.message}"
                )
            } catch (e: Exception) {
                // Handle other errors
                Log.e("AuthScreen", "Exception during Google Sign-In", e)
                snackbarHostState.showSnackbar(
                    message = "An error occurred: ${e.message}"
                )
            }
        }
    }

    // Handle auth success
    LaunchedEffect(authState) {
        Log.d("AuthScreen", "AuthState changed: $authState")
        when (authState) {
            is AuthState.Authenticated -> {
                Log.d("AuthScreen", "Authenticated, calling onAuthSuccess")
                onAuthSuccess()
            }
            is AuthState.NeedsProfileCompletion -> {
                Log.d("AuthScreen", "NeedsProfileCompletion, setting isSignUp = true")
                val user = (authState as AuthState.NeedsProfileCompletion).firebaseUser
                googleUser = user
                isSignUp = true
            }
            is AuthState.Error -> {
                Log.e("AuthScreen", "Auth Error: ${(authState as AuthState.Error).message}")
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                viewModel.clearError()
            }
            else -> {
                Log.d("AuthScreen", "AuthState: $authState")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isSignUp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                SignUpStepperScreen(
                    viewModel = viewModel,
                    isLoading = authState is AuthState.Loading,
                    onSwitchToSignIn = { 
                        isSignUp = false
                        googleUser = null
                        showEmailAuth = true
                    },
                    initialName = googleUser?.displayName ?: "",
                    initialEmail = googleUser?.email ?: "",
                    initialImageUri = googleUser?.photoUrl,
                    isGoogleSignIn = googleUser != null
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Header
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In Button (Primary) - Using Material 3 FilledTonalButton
                FilledTonalButton(
                    onClick = handleGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    // Google "G" logo - prominent size following Material 3 guidelines
                    Surface(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divider with "or"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "or continue with email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Email/Password Form
                if (!showEmailAuth) {
                    // Show button to reveal email form
                    OutlinedButton(
                        onClick = { showEmailAuth = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Form.email,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizes.medium)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Email")
                    }
                } else {
                    SignInForm(
                        viewModel = viewModel,
                        isLoading = authState is AuthState.Loading,
                        onSwitchToSignUp = { isSignUp = true }
                    )
                }
                
                // Switch between sign in/sign up
                if (showEmailAuth) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                isSignUp = true
                            }
                        ) {
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInForm(
    viewModel: AuthViewModel,
    isLoading: Boolean,
    onSwitchToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Form.email,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.medium)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Form.lock,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.medium)
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) AppIcons.Form.visibility else AppIcons.Form.visibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(IconSizes.medium)
                    )
                }
            },
           visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                viewModel.signIn(email.trim(), password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Sign In")
        }
    }
}
