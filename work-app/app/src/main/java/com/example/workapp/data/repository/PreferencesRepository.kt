package com.example.workapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling shared preferences
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "work_app_prefs"
        private const val KEY_LAST_VIEWED_APPLICATIONS_TIME = "last_viewed_applications_time"
    }

    /**
     * Get the timestamp when the user last viewed their applications
     */
    fun getLastViewedApplicationsTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_VIEWED_APPLICATIONS_TIME, 0L)
    }

    /**
     * Set the timestamp when the user last viewed their applications
     */
    fun setLastViewedApplicationsTime(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_VIEWED_APPLICATIONS_TIME, timestamp).apply()
    }
}
