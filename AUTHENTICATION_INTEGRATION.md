# Authentication Integration Guide

This document describes the authentication system integrated into the PeruWork Android app.

## Overview

The authentication system provides a complete user registration and login flow for both **Clients** (users seeking services) and **Craftsmen** (service providers). The system follows Material Design 3 guidelines and is fully integrated with the Convex backend.

## Architecture

### Frontend (Android)

#### 1. UI Layer - AuthScreen.kt
Location: `android-starter/app/src/main/java/com/example/androidstarter/ui/screens/AuthScreen.kt`

**Screens:**
- **Welcome Screen**: App branding and entry point
- **Role Selection**: User chooses between Client or Craftsman role
- **Sign-Up Screen**: Role-specific registration form
- **Login Screen**: Standard email/password login

**Features:**
- Smooth animated transitions between screens
- Form validation and input handling
- Loading states with progress indicators
- Proper keyboard navigation (Next/Done actions)
- Password visibility toggle
- Responsive design following Material Design 3

#### 2. Data Layer - AuthRepository.kt
Location: `android-starter/app/src/main/java/com/example/androidstarter/data/AuthRepository.kt`

**Responsibilities:**
- Communicates with Convex backend via ConvexClient
- Manages authentication state using SharedPreferences
- Provides reactive Flow-based API

**Key Methods:**
```kotlin
suspend fun signUp(...): Flow<Result<User>>
suspend fun login(...): Flow<Result<User>>
suspend fun getCurrentUser(): Flow<Result<User?>>
suspend fun logout(): Flow<Result<Boolean>>
```

#### 3. ViewModel - AuthViewModel.kt
Location: `android-starter/app/src/main/java/com/example/androidstarter/viewmodel/AuthViewModel.kt`

**State Management:**
```kotlin
data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)
```

**Features:**
- Reactive state using Kotlin StateFlow
- Automatic session restoration on app start
- Error handling and user feedback
- Loading state management

#### 4. Navigation Integration
Location: `android-starter/app/src/main/java/com/example/androidstarter/navigation/Navigation.kt`

**Routes:**
- `auth` - Authentication screens
- `craftsmen_list` - Main app screen (requires authentication)

**Flow:**
1. App starts → Check authentication status
2. Not authenticated → Show auth screen
3. Authenticated → Show main content
4. After successful login/signup → Navigate to main app

#### 5. Theme - Material Design 3
Location: `android-starter/app/src/main/java/com/example/androidstarter/ui/theme/`

**Updates:**
- Professional blue color palette
- Complete light and dark theme support
- All Material 3 color roles properly defined
- Smooth status bar transitions

### Backend (Convex)

#### Authentication Functions
Location: `convex-backend/convex/auth.ts`

**Mutations:**

1. **signUp**
   ```typescript
   args: {
     name: string,
     email: string,
     phone: string,
     location: string,
     password: string,
     role: "CLIENT" | "CRAFTSMAN",
     craft?: string,
     description?: string
   }
   ```
   - Creates new user in database
   - Validates email uniqueness
   - Sets role-specific defaults

2. **login**
   ```typescript
   args: {
     email: string,
     password: string
   }
   ```
   - Validates credentials
   - Returns user data

**Queries:**

1. **getCurrentUser**
   ```typescript
   args: { email: string }
   ```
   - Retrieves user by email
   - Used for session restoration

## User Roles

### CLIENT
**Purpose:** Users seeking services from craftsmen

**Required Fields:**
- name
- email
- phone
- location
- password

**Database Fields:**
```typescript
{
  role: "CLIENT",
  name: string,
  email: string,
  phone: string,
  location: string,
  profileImageUrl?: string
}
```

### CRAFTSMAN
**Purpose:** Service providers offering their skills

**Required Fields:**
- name
- email
- phone
- location
- password
- craft (specialization)

**Database Fields:**
```typescript
{
  role: "CRAFTSMAN",
  name: string,
  email: string,
  phone: string,
  location: string,
  craft: string,
  description: string,
  bio: string,
  experience: number,
  specialties: string[],
  rating: number,
  completedProjects: number,
  reviewCount: number,
  profileImageUrl?: string
}
```

## Session Management

**Storage:** SharedPreferences (`auth_prefs`)
**Key:** `user_email`

**Flow:**
1. User logs in/signs up
2. Email is saved to SharedPreferences
3. On app restart, AuthViewModel checks for saved email
4. If found, queries backend for user data
5. User is automatically logged in

**Logout:**
- Removes email from SharedPreferences
- Clears AuthState
- Navigates back to auth screen

## Security Notes

⚠️ **Important:** The current implementation stores passwords in plain text for demonstration purposes.

**For Production, you should:**
1. Hash passwords before storing (e.g., using bcrypt)
2. Implement JWT or session tokens
3. Use HTTPS for all communications
4. Consider using Convex Auth or a dedicated auth provider
5. Add password strength validation
6. Implement rate limiting for login attempts
7. Add email verification
8. Implement password reset functionality

## Testing

**Manual Testing Steps:**

1. **Sign Up as Client:**
   - Open app
   - Tap "Comenzar"
   - Select "Busco servicios"
   - Fill in all fields
   - Tap "Crear cuenta"
   - Verify navigation to main screen

2. **Sign Up as Craftsman:**
   - Same as above but select "Ofrezco servicios"
   - Additional field: craft/specialty
   - Verify all craftsman fields are populated

3. **Login:**
   - Use existing user credentials
   - Verify successful login
   - Check session persistence (close/reopen app)

4. **Logout:**
   - (To be implemented in UI)
   - Verify return to auth screen
   - Verify session cleared

## Future Enhancements

1. **Password Reset:** Implement forgot password flow
2. **Email Verification:** Verify email addresses
3. **Social Login:** Add Google/Facebook authentication
4. **Profile Pictures:** Allow users to upload avatars
5. **Two-Factor Authentication:** Add extra security layer
6. **Biometric Auth:** Support fingerprint/face recognition
7. **Account Deletion:** Allow users to delete their accounts
8. **Terms & Privacy:** Add acceptance during signup

## Files Modified/Created

### Created:
- `android-starter/app/src/main/java/com/example/androidstarter/ui/screens/AuthScreen.kt`
- `android-starter/app/src/main/java/com/example/androidstarter/data/AuthRepository.kt`
- `android-starter/app/src/main/java/com/example/androidstarter/viewmodel/AuthViewModel.kt`
- `convex-backend/convex/auth.ts`

### Modified:
- `android-starter/app/src/main/java/com/example/androidstarter/MainActivity.kt`
- `android-starter/app/src/main/java/com/example/androidstarter/navigation/Navigation.kt`
- `android-starter/app/src/main/java/com/example/androidstarter/ui/theme/Color.kt`
- `android-starter/app/src/main/java/com/example/androidstarter/ui/theme/Theme.kt`

## Support

For questions or issues, refer to:
- [Material Design 3 Guidelines](https://m3.material.io/)
- [Convex Documentation](https://docs.convex.dev/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)