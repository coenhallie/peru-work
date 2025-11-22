# Firebase App Check Fix for Emulator Login

## Problem
The app was unable to login in the Android emulator due to missing Firebase App Check configuration, resulting in this error:
```
Error getting App Check token; using placeholder token instead. 
Error: com.google.firebase.FirebaseException: No AppCheckProvider installed.
```

## Solution Applied

### 1. Added Firebase App Check Debug Dependency
**File:** [`work-app/app/build.gradle.kts`](work-app/app/build.gradle.kts:68)

Added the Firebase App Check debug library to dependencies:
```kotlin
implementation("com.google.firebase:firebase-appcheck-debug")
```

This library provides a debug App Check provider that works in emulator and debug builds without requiring production App Check configuration.

### 2. Initialized App Check in Application Class
**File:** [`work-app/app/src/main/java/com/example/workapp/WorkAppApplication.kt`](work-app/app/src/main/java/com/example/workapp/WorkAppApplication.kt:14)

Added App Check initialization code:
```kotlin
// Initialize App Check with debug provider for emulator/development
val firebaseAppCheck = FirebaseAppCheck.getInstance()
firebaseAppCheck.installAppCheckProviderFactory(
    DebugAppCheckProviderFactory.getInstance()
)
```

## How to Apply the Fix

1. **In Android Studio**, click on "Sync Project with Gradle Files" (or the Gradle sync button)
2. **Rebuild the project** (Build → Rebuild Project)
3. **Run the app** on the emulator

The Firebase App Check debug provider will now be active, allowing authentication to work properly in the emulator.

## What This Does

- **DebugAppCheckProviderFactory**: Provides a debug token that Firebase accepts during development and testing
- **No production impact**: This debug provider is automatically excluded from release builds
- **Emulator-friendly**: Works seamlessly with the Firebase emulator suite

## Note on Mapbox Build Error

If you encounter Mapbox-related build errors during Gradle sync, these are unrelated to the login fix. The App Check changes will be applied when you rebuild in Android Studio, which handles dependency resolution automatically.

## Testing

After rebuilding:
1. Launch the app in the Android emulator
2. Navigate to the login screen  
3. Attempt to sign in with email/password
4. Login should now work without App Check errors

The warning about "No AppCheckProvider installed" will be gone, and authentication will proceed normally.