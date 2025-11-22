package com.example.workapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.cloudinary.android.MediaManager
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import dagger.hilt.android.HiltAndroidApp
import java.util.HashMap

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
        // Initialize Cloudinary
        initCloudinary()
    }

    private fun initCloudinary() {
        val cloudinaryUrl = BuildConfig.CLOUDINARY_URL
        if (cloudinaryUrl.isNotEmpty()) {
            try {
                val config = HashMap<String, Any>()
                // Parse the URL manually to avoid bringing in the whole Cloudinary core if not needed, 
                // OR use the Cloudinary class if available.
                // Format: cloudinary://api_key:api_secret@cloud_name
                val uri = android.net.Uri.parse(cloudinaryUrl)
                val userInfo = uri.userInfo?.split(":")
                if (userInfo != null && userInfo.size == 2) {
                    config["api_key"] = userInfo[0]
                    config["api_secret"] = userInfo[1]
                    config["cloud_name"] = uri.host ?: ""
                    config["secure"] = true
                    MediaManager.init(this, config)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}