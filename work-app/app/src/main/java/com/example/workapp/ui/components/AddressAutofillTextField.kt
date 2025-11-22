package com.example.workapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.workapp.BuildConfig
import com.example.workapp.ui.theme.AppIcons
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mapbox Geocoding API response models
 */
data class MapboxGeocodingResponse(
    @SerializedName("features") val features: List<MapboxFeature>
)

data class MapboxFeature(
    @SerializedName("id") val id: String,
    @SerializedName("place_name") val placeName: String,
    @SerializedName("text") val text: String,
    @SerializedName("place_type") val placeType: List<String>?,
    @SerializedName("geometry") val geometry: MapboxGeometry?
)

data class MapboxGeometry(
    @SerializedName("coordinates") val coordinates: List<Double>
)

/**
 * Mapbox Geocoding API service interface
 */
interface MapboxGeocodingService {
    @GET("geocoding/v5/mapbox.places/{query}.json")
    suspend fun searchPlaces(
        @Path("query") query: String,
        @Query("access_token") accessToken: String,
        @Query("country") country: String = "pe",
        @Query("limit") limit: Int = 5,
        @Query("autocomplete") autocomplete: Boolean = true,
        @Query("types") types: String = "address,place,locality,neighborhood,poi"
    ): MapboxGeocodingResponse
}

/**
 * A text field with Mapbox address autocomplete functionality
 * Provides address suggestions as the user types using Mapbox Geocoding API
 */
@OptIn(FlowPreview::class)
@Composable
fun AddressAutofillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Location",
    placeholder: String = "Start typing an address...",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    var suggestions by remember { mutableStateOf<List<MapboxFeature>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    val searchFlow = remember { MutableStateFlow("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Mapbox Geocoding API service
    val geocodingService = remember {
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
    
    // Debounce search requests
    LaunchedEffect(Unit) {
        searchFlow
            .debounce(300)
            .distinctUntilChanged()
            .collect { query ->
                if (query.isNotBlank() && geocodingService != null && BuildConfig.MAPBOX_PUBLIC_TOKEN.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            val response = geocodingService.searchPlaces(
                                query = query,
                                accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN,
                                country = "pe",
                                limit = 5
                            )
                            suggestions = response.features
                            showSuggestions = suggestions.isNotEmpty()
                        } catch (e: Exception) {
                            suggestions = emptyList()
                            showSuggestions = false
                        }
                    }
                } else {
                    suggestions = emptyList()
                    showSuggestions = false
                }
            }
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                searchFlow.value = newValue
                showSuggestions = newValue.isNotBlank()
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Form.location,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            enabled = enabled
        )
        
        // Suggestions dropdown
        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                onValueChange(suggestion.placeName)
                                showSuggestions = false
                                suggestions = emptyList()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: MapboxFeature,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = suggestion.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = suggestion.placeName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}