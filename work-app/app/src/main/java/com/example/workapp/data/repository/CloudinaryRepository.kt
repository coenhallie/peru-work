package com.example.workapp.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.workapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for handling Cloudinary image uploads
 */
@Singleton
class CloudinaryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    init {
        // Initialize Cloudinary MediaManager if not already initialized
        try {
            val cloudinaryUrl = BuildConfig.CLOUDINARY_URL
            if (cloudinaryUrl.isNotEmpty()) {
                // Parse cloudinary URL format: cloudinary://API_KEY:API_SECRET@CLOUD_NAME
                val uri = android.net.Uri.parse(cloudinaryUrl)
                val cloudName = uri.host ?: ""
                val apiKey = uri.userInfo?.split(":")?.get(0) ?: ""
                val apiSecret = uri.userInfo?.split(":")?.getOrNull(1) ?: ""
                
                val config = hashMapOf<String, Any>()
                config["cloud_name"] = cloudName
                config["api_key"] = apiKey
                config["api_secret"] = apiSecret
                
                MediaManager.init(context, config)
            }
        } catch (e: Exception) {
            // MediaManager already initialized or error - ignore
        }
    }
    
    /**
     * Upload an image to Cloudinary
     * @param imageUri The URI of the image to upload
     * @param folder The folder in Cloudinary to upload to (default: "chat_images")
     * @return The secure URL of the uploaded image
     */
    suspend fun uploadImage(
        imageUri: Uri,
        folder: String = "chat_images"
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val requestId = MediaManager.get()
                .upload(imageUri)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("quality", "auto:good")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }
                    
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Progress update - could be used for showing upload progress
                    }
                    
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            if (continuation.isActive) {
                                continuation.resume(Result.success(secureUrl))
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    Exception("Failed to get secure URL from upload result")
                                )
                            }
                        }
                    }
                    
                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Upload failed: ${error.description}")
                            )
                        }
                    }
                    
                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Upload will be retried
                    }
                })
                .dispatch()
            
            continuation.invokeOnCancellation {
                // Cancel the upload if coroutine is cancelled
                try {
                    MediaManager.get().cancelRequest(requestId)
                } catch (e: Exception) {
                    // Ignore cancellation errors
                }
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
}