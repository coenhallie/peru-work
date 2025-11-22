package com.example.workapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WorkAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize App Check with debug provider for emulator/development
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        
        // Note: Mapbox initialization is handled automatically via the access token
        // provided in gradle.properties (MAPBOX_PUBLIC_TOKEN) or strings.xml (mapbox_access_token).
        // The JobDetailScreen checks BuildConfig.MAPBOX_PUBLIC_TOKEN to conditionally display maps.
    }
}