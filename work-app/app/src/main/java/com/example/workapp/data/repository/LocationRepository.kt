package com.example.workapp.data.repository

import com.example.workapp.BuildConfig
import com.example.workapp.ui.components.MapboxGeocodingService
import com.mapbox.geojson.Point
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Repository for location-related operations
 */
@Singleton
class LocationRepository @Inject constructor() {

    private val geocodingService: MapboxGeocodingService? by lazy {
        if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotEmpty()) {
            Retrofit.Builder()
                .baseUrl("https://api.mapbox.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MapboxGeocodingService::class.java)
        } else {
            null
        }
    }

    /**
     * Get coordinates for a given address string
     */
    suspend fun getCoordinates(address: String): Point? {
        if (address.isBlank() || geocodingService == null) return null

        return try {
            val response = geocodingService!!.searchPlaces(
                query = address,
                accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN,
                limit = 1
            )
            
            response.features.firstOrNull()?.geometry?.coordinates?.let { coords ->
                if (coords.size >= 2) {
                    Point.fromLngLat(coords[0], coords[1])
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate distance between two points in kilometers using Haversine formula
     */
    fun calculateDistanceKm(point1: Point, point2: Point): Double {
        val lat1 = point1.latitude()
        val lon1 = point1.longitude()
        val lat2 = point2.latitude()
        val lon2 = point2.longitude()

        val r = 6371.0 // Earth radius in km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}