# WorkApp - Craftsmen Marketplace

A modern Android application connecting skilled craftsmen (gardeners, painters, carpenters, etc.) with clients who need their services.

## 🎯 Features

### ✨ Core Functionality
- **User Authentication** - Email/password authentication via Firebase
- **Role-Based Access** - Separate experiences for Clients and Craftsmen
- **Craftsmen Discovery** - Browse and search skilled professionals
- **Profile Management** - Detailed craftsman profiles with ratings and reviews
- **Real-time Updates** - Live data synchronization with Firestore

### 🎨 Design & UX
- **Material Design 3** - Modern, clean UI following Google's latest design guidelines
- **Material You** - Dynamic theming support for Android 12+
- **Smooth Animations** - Polished transitions and micro-interactions
- **Responsive Layout** - Adaptive design for different screen sizes
- **Intuitive Navigation** - Clear navigation patterns inspired by Uber and Airbnb

## 🏗️ Architecture

### Tech Stack
- **Language:** Kotlin 2.1.0
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Injection:** Hilt
- **Backend:** Firebase (Authentication + Firestore)
- **Navigation:** Jetpack Navigation Compose
- **Async:** Kotlin Coroutines + Flow
- **Image Loading:** Coil

### Project Structure
```
work-app/
├── app/src/main/java/com/example/workapp/
│   ├── data/
│   │   ├── model/               # Data models
│   │   │   ├── User.kt
│   │   │   └── Job.kt
│   │   └── repository/          # Data repositories
│   │       ├── AuthRepository.kt
│   │       ├── CraftsmenRepository.kt
│   │       └── JobRepository.kt
│   ├── di/                      # Dependency Injection
│   │   └── AppModule.kt
│   ├── navigation/              # Navigation setup
│   │   └── NavGraph.kt
│   ├── ui/
│   │   ├── screens/             # UI screens
│   │   │   ├── welcome/         # Onboarding screen
│   │   │   ├── auth/            # Authentication screens
│   │   │   ├── home/            # Craftsmen listing
│   │   │   └── craftsman/       # Craftsman detail
│   │   ├── theme/               # Material 3 theme
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Theme.kt
│   │   └── viewmodel/           # ViewModels
│   │       ├── AuthViewModel.kt
│   │       └── CraftsmenViewModel.kt
│   ├── MainActivity.kt
│   └── WorkAppApplication.kt
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17 or newer
- Android SDK API 26+ (Android 8.0+)
- Firebase project with Authentication and Firestore enabled

### Firebase Setup

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Create a new project or use existing one
   - Add an Android app with package name: `com.example.workapp`

2. **Download google-services.json**
   - Download `google-services.json` from Firebase Console
   - Place it in `work-app/app/` directory

3. **Enable Firebase Services**
   - Enable **Authentication** → Email/Password provider
   - Enable **Cloud Firestore** → Create database in production/test mode

4. **Firestore Security Rules** (Optional for development)
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

### Installation

1. **Clone the repository**
   ```bash
   cd work-app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open" and navigate to `work-app` folder
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```
   Or click the "Run" button in Android Studio

## 📱 App Flow

### 1. Welcome Screen
- Animated onboarding with smooth transitions
- "Get Started" button to begin

### 2. Authentication
- **Sign Up Tab**
  - Role selection (Client or Craftsman)
  - Full name, email, phone, location
  - Craftsman-specific fields (craft, bio)
  - Password with visibility toggle
  
- **Sign In Tab**
  - Email and password fields
  - Simple, clean interface

### 3. Home Screen (Craftsmen Listing)
- Search bar for finding craftsmen
- Category filters (Plumber, Electrician, etc.)
- Craftsman cards showing:
  - Profile photo
  - Name and craft
  - Rating and review count
  - Years of experience
  
### 4. Craftsman Detail Screen
- Full profile information
- About section with bio
- Specialties list
- Contact information
- "Request Service" button (ready for booking implementation)

## 🎨 Design Principles

### Color Scheme
- **Primary:** Professional Blue (#1976D2)
- **Secondary:** Teal (#26A69A)
- **Background:** Light Grey (#FAFAFA)
- **Surface:** White (#FFFFFF)

### Typography
- Material 3 type scale
- Clear hierarchy with font weights
- Readable sizes for all screen sizes

### Components
- Cards with subtle elevation
- Rounded corners for modern feel
- Generous spacing and padding
- Clear call-to-action buttons

## 🔐 Security

### Authentication
- Firebase Authentication handles:
  - Secure password storage
  - Email verification (can be enabled)
  - Session management
  - Token refresh

### Data Access
- User authentication required for all operations
- Role-based data filtering
- Firestore security rules (should be configured in production)

## 📊 Database Schema

### Users Collection
```javascript
{
  id: string,                    // Firebase Auth UID
  email: string,
  name: string,
  phone: string,
  location: string,
  role: "CLIENT" | "CRAFTSMAN",
  profileImageUrl: string?,
  createdAt: number,
  
  // Craftsman-specific fields
  craft: string?,
  bio: string?,
  experience: number?,
  rating: number?,
  reviewCount: number?,
  completedProjects: number?,
  specialties: string[]?,
  hourlyRate: number?,
  availability: string?
}
```

### Jobs Collection (Schema ready, UI pending)
```javascript
{
  id: string,
  title: string,
  description: string,
  category: string,
  location: string,
  clientId: string,
  clientName: string,
  craftsmanId: string?,
  craftsmanName: string?,
  status: "OPEN" | "PENDING" | "ACCEPTED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED",
  budget: number?,
  createdAt: number,
  updatedAt: number,
  ...
}
```

## 🧪 Testing

### Manual Testing Checklist
- [ ] Sign up as Client
- [ ] Sign up as Craftsman
- [ ] Sign in with existing account
- [ ] Browse craftsmen list
- [ ] Search for specific craft
- [ ] Filter by category
- [ ] View craftsman detail
- [ ] Sign out

### Seeding Test Data
Use the Firebase Console or the seed script in the parent directory to add sample craftsmen:
```bash
# From project root
node seed-craftsmen.js
```

## 🚧 Future Enhancements

### Planned Features
- [ ] Job booking and management
- [ ] In-app messaging between clients and craftsmen
- [ ] Payment integration
- [ ] Review and rating system
- [ ] Push notifications
- [ ] Image upload for profiles and jobs
- [ ] Google Maps integration for location
- [ ] Availability calendar
- [ ] Portfolio gallery for craftsmen
- [ ] Advanced search filters
- [ ] Favorites/bookmarks
- [ ] Dark mode toggle

### Technical Improvements
- [ ] Unit tests with JUnit
- [ ] UI tests with Compose Testing
- [ ] Offline support with Room
- [ ] Analytics integration
- [ ] Crash reporting (Firebase Crashlytics)
- [ ] Performance monitoring
- [ ] CI/CD pipeline

## 📝 Key Dependencies

```kotlin
// Core
androidx.core:core-ktx:1.15.0
androidx.lifecycle:lifecycle-runtime-ktx:2.8.7
androidx.activity:activity-compose:1.9.3

// Compose
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.navigation:navigation-compose:2.8.5

// Firebase
com.google.firebase:firebase-bom:34.3.0
com.google.firebase:firebase-auth
com.google.firebase:firebase-firestore

// Hilt
com.google.dagger:hilt-android:2.51.1
androidx.hilt:hilt-navigation-compose:1.2.0

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0

// Image Loading
io.coil-kt:coil-compose:2.7.0
```

## 🤝 Contributing

This is a portfolio project, but suggestions and improvements are welcome!

## 📄 License

This project is created as a demonstration of modern Android development practices.

## 👨‍💻 Developer Notes

### Development Environment
- Built with Android Studio Hedgehog+
- Tested on Android 8.0+ (API 26+)
- Target SDK: 36
- Min SDK: 26

### Code Quality
- Follows Kotlin coding conventions
- Uses Material Design 3 guidelines
- Implements MVVM architecture pattern
- Leverages Jetpack Compose best practices

---

**Created with ❤️ using Kotlin and Jetpack Compose**