# Clerk + Convex Authentication Integration Guide

Complete guide for integrating Clerk authentication with Convex backend and biometric authentication in your Android application.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Clerk Setup](#clerk-setup)
3. [Convex Backend Configuration](#convex-backend-configuration)
4. [Android App Configuration](#android-app-configuration)
5. [Using Authentication](#using-authentication)
6. [Biometric Authentication](#biometric-authentication)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Android Studio Arctic Fox or newer
- Kotlin 1.9.0+
- Android SDK with minimum API level 26 (Android 8.0)
- Node.js and npm installed
- Convex account (https://www.convex.dev/)
- Clerk account (https://clerk.com/)

---

## Clerk Setup

### 1. Create a Clerk Application

1. Go to [Clerk Dashboard](https://dashboard.clerk.com/)
2. Click "Add application"
3. Choose a name for your application (e.g., "Peru Work")
4. Select your preferred social login providers or use Email/Password
5. Click "Create application"

### 2. Get Your Clerk Publishable Key

1. In the Clerk Dashboard, navigate to "API Keys"
2. Copy your **Publishable Key** (starts with `pk_test_` or `pk_live_`)
3. This will be used in `gradle.properties`

### 3. Get Your Clerk JWT Issuer Domain

1. In Clerk Dashboard, go to "JWT Templates"
2. Create a new template called "convex"
3. Note your JWT Issuer URL (format: `https://your-instance.clerk.accounts.dev`)
4. This will be used in Convex environment variables

### 4. Configure Clerk for Android

1. In Clerk Dashboard, go to "Settings" → "Mobile"
2. Add your Android package name: `com.example.androidstarter`
3. Configure deep linking if needed

---

## Convex Backend Configuration

### 1. Set Up Convex Environment Variables

1. Go to your [Convex Dashboard](https://dashboard.convex.dev/)
2. Select your deployment: `adjoining-hamster-879`
3. Navigate to "Settings" → "Environment Variables"
4. Add the following variable:
   - **Name**: `CLERK_JWT_ISSUER_DOMAIN`
   - **Value**: Your Clerk JWT Issuer URL (e.g., `https://your-instance.clerk.accounts.dev`)

### 2. Verify Convex Configuration

The following files have been configured in `convex-backend/convex/`:

- **`auth.config.ts`**: Configures Clerk as the authentication provider
- **`schema.ts`**: Updated to include `clerkId` field for users
- **`clerkAuth.ts`**: New authentication functions for Clerk integration

### 3. Deploy Convex Backend

```bash
cd convex-backend
npx convex dev
```

Wait for the deployment to complete. You should see a success message.

---

## Android App Configuration

### 1. Configure Clerk Publishable Key

Edit `android-starter/gradle.properties`:

```properties
CLERK_PUBLISHABLE_KEY=pk_test_your_actual_publishable_key_here
```

**⚠️ Important**: Replace `pk_test_your_publishable_key_here` with your actual Clerk Publishable Key from the Clerk Dashboard.

### 2. Sync Gradle

Open the project in Android Studio and sync Gradle files:
- Click "Sync Now" when prompted
- Or go to File → Sync Project with Gradle Files

### 3. Required Permissions

The following permissions are already configured in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

---

## Using Authentication

### Authentication Repository

The `ClerkAuthRepository` provides the following methods using the official Clerk Android SDK API:

#### Sign Up with Email

```kotlin
val clerkAuth = ClerkAuthRepository(context)

viewModelScope.launch {
    clerkAuth.signUpWithEmail(
        email = "user@example.com",
        password = "SecurePassword123!",
        firstName = "John",
        lastName = "Doe"
    ).collect { result ->
        result.onSuccess { signUpResult ->
            when (signUpResult) {
                is SignUpResult.Complete -> {
                    // User created and verified, no code needed
                    val user = signUpResult.user
                    // Initialize Convex with auth
                    ConvexClientProvider.initializeWithAuth(context)
                }
                is SignUpResult.NeedsVerification -> {
                    // Show verification code input screen
                    // User will receive an email with the code
                }
            }
        }.onFailure { error ->
            // Handle error
            Toast.makeText(context, "Sign up error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### Verify Email

```kotlin
viewModelScope.launch {
    clerkAuth.verifyEmail(code = "123456").collect { result ->
        result.onSuccess { user ->
            // Email verified, user is now signed in
            // Initialize Convex with auth
            ConvexClientProvider.initializeWithAuth(context)
        }.onFailure { error ->
            // Handle error
            Toast.makeText(context, "Verification error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### Sign In

```kotlin
viewModelScope.launch {
    clerkAuth.signInWithEmail(
        email = "user@example.com",
        password = "SecurePassword123!"
    ).collect { result ->
        result.onSuccess { user ->
            // User signed in successfully
            ConvexClientProvider.initializeWithAuth(context)
        }.onFailure { error ->
            // Handle error
            Toast.makeText(context, "Sign in error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### Sign Out

```kotlin
viewModelScope.launch {
    clerkAuth.signOut().collect { result ->
        result.onSuccess {
            // User signed out
            ConvexClientProvider.clearAuth()
        }.onFailure { error ->
            Toast.makeText(context, "Sign out error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### Get Current User

```kotlin
// Synchronous access
val currentUser = clerkAuth.getCurrentUser()

// Or use Flow for reactive updates
viewModelScope.launch {
    clerkAuth.observeCurrentUser().collect { user ->
        if (user != null) {
            // User is signed in
            Log.d("Auth", "Signed in as: ${user.firstName} ${user.lastName}")
        } else {
            // User is not signed in
            Log.d("Auth", "Not signed in")
        }
    }
}
```

### Check Authentication State

Use Clerk's built-in flows for reactive auth state management:

```kotlin
import com.clerk.api.Clerk
import kotlinx.coroutines.flow.combine

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            _uiState.value = when {
                !isInitialized -> AuthUiState.Loading
                user != null -> AuthUiState.SignedIn(user)
                else -> AuthUiState.SignedOut
            }
        }.launchIn(viewModelScope)
    }
}

sealed interface AuthUiState {
    object Loading : AuthUiState
    data class SignedIn(val user: ClerkUser) : AuthUiState
    object SignedOut : AuthUiState
}
```

### Syncing User with Convex

After successful authentication with Clerk, sync the user with Convex:

```kotlin
viewModelScope.launch {
    val user = Clerk.user ?: return@launch
    
    // Get Convex client
    val convexClient = ConvexClientProvider.client

    // Sync user data with Convex
    val userId = convexClient.mutation<String>(
        name = "clerkAuth:syncUser",
        args = buildJsonObject {
            put("clerkId", user.id)
            put("email", user.primaryEmailAddress?.emailAddress ?: "")
            put("name", "${user.firstName ?: ""} ${user.lastName ?: ""}".trim())
            put("phone", user.primaryPhoneNumber?.phoneNumber ?: "")
            put("profileImageUrl", user.imageUrl ?: "")
        }
    )
}
```

---

## Biometric Authentication

### Checking Biometric Availability

```kotlin
val biometricAuth = BiometricAuthManager(context)

when (val availability = biometricAuth.canAuthenticateWithBiometrics()) {
    is BiometricAvailability.Available -> {
        // Biometric authentication is available
    }
    is BiometricAvailability.NoneEnrolled -> {
        // User needs to enroll biometrics in device settings
        Toast.makeText(context, availability.getMessage(), Toast.LENGTH_LONG).show()
    }
    else -> {
        // Biometric not available - use password fallback
        Toast.makeText(context, availability.getMessage(), Toast.LENGTH_SHORT).show()
    }
}
```

### Enabling Biometric Login

After user signs in successfully, offer to enable biometric login:

```kotlin
val biometricAuth = BiometricAuthManager(context)
val clerkAuth = ClerkAuthRepository(context)

biometricAuth.enableBiometricAuth(
    activity = requireActivity() as FragmentActivity,
    credentials = userEmail, // Store email for later use
    clerkAuthRepository = clerkAuth,
    onSuccess = {
        Toast.makeText(context, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
    },
    onError = { error ->
        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
    }
)
```

### Authenticating with Biometrics

On subsequent app launches, offer biometric authentication:

```kotlin
biometricAuth.authenticateWithBiometrics(
    activity = requireActivity() as FragmentActivity,
    onSuccess = { email ->
        // Use the retrieved email to sign in
        // Then sign in the user with Clerk using stored session
        Toast.makeText(context, "Biometric auth successful!", Toast.LENGTH_SHORT).show()
    },
    onError = { error ->
        Toast.makeText(context, "Biometric auth failed: $error", Toast.LENGTH_SHORT).show()
    },
    onFallbackToPassword = {
        // Show password login screen
    }
)
```

### Disabling Biometric Login

```kotlin
biometricAuth.disableBiometricAuth()
Toast.makeText(context, "Biometric login disabled", Toast.LENGTH_SHORT).show()
```

---

## Testing

### 1. Test Email/Password Authentication

1. Run the app on a device or emulator
2. Try signing up with a new email address
3. Check your email for the verification code
4. Enter the code to complete sign-up
5. Sign out and sign back in with the same credentials

### 2. Test Biometric Authentication

**Prerequisites**: Device must have biometrics enrolled (fingerprint or face)

1. Sign in with email/password
2. Enable biometric authentication when prompted
3. Complete the biometric authentication prompt
4. Sign out
5. On next launch, try signing in with biometrics

**Testing on Emulator**:
- Use Android Studio emulator with API 28+
- Go to Settings → Security → Fingerprint
- Add a fingerprint (you can use mouse clicks to simulate)

### 3. Test Convex Integration

```kotlin
// After signing in, test Convex queries
viewModelScope.launch {
    val user = convexClient.query<User>(
        name = "clerkAuth:getCurrentUser",
        args = buildJsonObject {}
    )

    Log.d("TEST", "Current user: ${user?.name}")
}
```

---

## Troubleshooting

### Clerk Issues

**Error: "Invalid publishable key"**
- Verify your `CLERK_PUBLISHABLE_KEY` in `gradle.properties`
- Make sure it starts with `pk_test_` or `pk_live_`
- Rebuild the project after changing gradle.properties

**Error: "Sign up failed"**
- Check Clerk Dashboard → Settings → Email settings
- Ensure email delivery is configured
- Check for valid email address format

### Convex Issues

**Error: "Unauthenticated"**
- Ensure `CLERK_JWT_ISSUER_DOMAIN` is set in Convex Dashboard
- Verify the JWT issuer URL matches your Clerk instance
- Make sure `ConvexClientProvider.initializeWithAuth()` was called after sign-in

**Convex dev not connecting**:
```bash
cd convex-backend
npx convex dev --once  # Run once to verify connection
npx convex dev  # Then run in watch mode
```

### Biometric Issues

**Error: "No biometric hardware"**
- Test on a physical device with fingerprint/face recognition
- Or use an emulator with API 28+ and enroll a fingerprint

**Error: "Biometric authentication failed"**
- Check that USE_BIOMETRIC permission is in AndroidManifest.xml
- Verify device has biometrics enrolled in Settings
- Try re-enrolling biometrics on the device

**Biometric prompt doesn't show**:
- Ensure you're using `FragmentActivity` (not just `Activity`)
- Check that biometric hardware is available
- Verify androidx.biometric dependency is included

### Build Issues

**BuildConfig not found**:
- Make sure `buildConfig = true` is set in `buildFeatures` in app/build.gradle.kts
- Clean and rebuild: Build → Clean Project → Rebuild Project

**Dependencies not resolving**:
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

---

## Security Best Practices

1. **Never commit API keys**: Keep `CLERK_PUBLISHABLE_KEY` in `gradle.properties` which should be in `.gitignore` for production
2. **Use environment-specific keys**: Use test keys during development, production keys only for release builds
3. **Implement certificate pinning**: For production apps, implement SSL certificate pinning
4. **Regular security audits**: Periodically review Clerk dashboard for suspicious activity
5. **Biometric fallback**: Always provide password fallback for biometric authentication
6. **Token refresh**: Implement token refresh logic for long-running sessions

---

## Next Steps

1. Customize the authentication UI to match your app's design
2. Implement proper error handling and user feedback
3. Add analytics to track authentication flows
4. Implement social sign-in (Google, Facebook, etc.) via Clerk
5. Add multi-factor authentication (MFA) support
6. Implement role-based access control using Convex auth context

---

## Additional Resources

- [Clerk Android Documentation](https://clerk.com/docs/quickstarts/android)
- [Convex Authentication Guide](https://docs.convex.dev/auth)
- [Android Biometric API Guide](https://developer.android.com/training/sign-in/biometric-auth)
- [Clerk Community](https://discord.com/invite/clerk)
- [Convex Discord](https://discord.gg/convex)

---

## Support

If you encounter issues:

1. Check this guide's troubleshooting section
2. Review Clerk and Convex documentation
3. Check the GitHub issues for similar problems
4. Join the Clerk or Convex Discord communities for help

---

**Last Updated**: 2025-01-15
**Version**: 2.0.0 (Updated for official Clerk Android SDK patterns)