# Firebase Setup Guide

This application has been migrated from Convex + Clerk to Firebase Authentication and Firestore.

## Prerequisites

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Register your Android app with the package name: `com.example.workapp`

## Configuration Steps

### 1. Download google-services.json

1. Go to your Firebase project in the Firebase Console
2. Click on the Android app you registered
3. Download the `google-services.json` file
4. Replace the placeholder file at `android-starter/app/google-services.json` with your downloaded file

### 2. Enable Authentication

1. In Firebase Console, go to **Authentication** > **Sign-in method**
2. Enable **Email/Password** authentication
3. (Optional) Enable other sign-in providers as needed

### 3. Set up Firestore Database

1. In Firebase Console, go to **Firestore Database**
2. Click **Create database**
3. Choose production mode or test mode (test mode for development)
4. Select a location for your database

### 4. Firestore Security Rules

Add the following security rules in **Firestore Database** > **Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can read/write their own data
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Jobs collection
    match /jobs/{jobId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
        (resource.data.clientId == request.auth.uid || 
         resource.data.assignedCraftsmanId == request.auth.uid);
      allow delete: if request.auth != null && 
        resource.data.clientId == request.auth.uid;
    }
    
    // Craftsmen collection (for searching)
    match /craftsmen/{craftsmanId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == craftsmanId;
    }
  }
}
```

## Migration Notes

### Removed Dependencies
- Clerk Authentication SDK
- Convex Android SDK

### Added Dependencies
- Firebase Authentication
- Firebase Firestore
- Firebase BoM (Bill of Materials) for version management

### Key Changes

1. **Authentication**: Migrated from Clerk to Firebase Authentication with email/password
2. **Database**: Migrated from Convex to Firebase Firestore
3. **Data Structure**: 
   - Users are now authenticated via Firebase Auth
   - User profile data stored in Firestore `/users/{userId}` collection
   - Jobs stored in Firestore `/jobs/{jobId}` collection
   - Craftsmen profiles stored in Firestore `/craftsmen/{userId}` collection

### Build Configuration

The app now uses:
- Google Services plugin for Firebase integration
- Firebase BoM version 34.3.0 (latest stable as of migration)
- Kotlin Coroutines Play Services for async Firebase operations

## Testing

1. Build and run the app
2. Register a new user with email/password
3. Complete the profile setup (role selection)
4. Test authentication persistence across app restarts

## Production Checklist

- [ ] Replace google-services.json with production configuration
- [ ] Update Firestore security rules for production
- [ ] Enable Firebase App Check for additional security
- [ ] Set up Firebase Analytics (optional)
- [ ] Configure proper authentication error handling
- [ ] Implement password reset flow
- [ ] Add email verification (optional)

## Support

For Firebase-specific issues, refer to:
- [Firebase Documentation](https://firebase.google.com/docs)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Cloud Firestore](https://firebase.google.com/docs/firestore)