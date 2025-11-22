# Mapbox Integration Guide

## Overview
This guide explains how to complete the Mapbox Maps integration that has replaced Google Maps in the job listing detail screen.

## Changes Made

### 1. Dependencies Updated
- **Removed**: Google Maps dependencies (`play-services-maps`, `maps-compose`)
- **Added**: Mapbox Maps SDK dependencies
  - `com.mapbox.maps:android:11.12.0-beta.1`
  - `com.mapbox.extension:maps-compose:11.12.0-beta.1`

### 2. Configuration Files Modified
- [`settings.gradle.kts`](settings.gradle.kts): Added Mapbox Maven repository with authentication
- [`gradle.properties`](gradle.properties): Added placeholders for Mapbox access tokens
- [`app/build.gradle.kts`](app/build.gradle.kts): Added BuildConfig field for runtime token

### 3. Code Changes
- [`JobDetailScreen.kt`](app/src/main/java/com/example/workapp/ui/screens/jobs/JobDetailScreen.kt): Replaced Google Maps implementation with Mapbox

## Setup Instructions

### Step 1: Get Mapbox Access Tokens

1. Create a free Mapbox account at [https://account.mapbox.com/](https://account.mapbox.com/)

2. **Get your Public Token**:
   - Go to [https://account.mapbox.com/access-tokens/](https://account.mapbox.com/access-tokens/)
   - Copy your default public token (starts with `pk.`)
   - This token is used for runtime map display

3. **Create a Downloads Token**:
   - Click "Create a token" button
   - Give it a name like "Downloads Token"
   - Under "Secret scopes", check `DOWNLOADS:READ`
   - Click "Create token"
   - Copy the token (starts with `sk.`)
   - **Important**: Save this token securely - you won't be able to see it again!

### Step 2: Configure Tokens

Open [`gradle.properties`](gradle.properties) and replace the placeholder values:

```properties
# Mapbox Configuration
MAPBOX_DOWNLOADS_TOKEN=sk.YOUR_ACTUAL_DOWNLOADS_TOKEN_HERE
MAPBOX_PUBLIC_TOKEN=pk.YOUR_ACTUAL_PUBLIC_TOKEN_HERE
```

### Step 3: Sync Gradle

1. In Android Studio, click "Sync Project with Gradle Files" (elephant icon in toolbar)
2. Wait for the sync to complete
3. Resolve any dependency issues if they arise

### Step 4: Build and Run

1. Clean the project: `Build > Clean Project`
2. Rebuild: `Build > Rebuild Project`
3. Run the app on an emulator or physical device

## Testing the Integration

### 1. Navigate to Job Details
- Open the app
- Navigate to any job listing
- Click on a job to view its details

### 2. Verify Map Display
- The map should display at the top of the job detail screen
- You should see a Mapbox Streets map centered on Lima, Peru (default location)
- A blue circle marker should indicate the job location
- The location name should appear in a label overlay at the bottom-left of the map

### 3. Map Interactions
- Try zooming in/out on the map (pinch gestures)
- Try panning the map (drag gestures)
- Verify the map responds smoothly to touch interactions

## Troubleshooting

### Issue: Map Not Displaying
- **Check**: Tokens are correctly set in `gradle.properties`
- **Check**: Project has been synced and rebuilt
- **Check**: Internet connection is available (maps require data)
- **Check**: BuildConfig.MAPBOX_PUBLIC_TOKEN is not empty in build output

### Issue: Build Errors
- **Common**: "Unauthorized" errors indicate the MAPBOX_DOWNLOADS_TOKEN is incorrect
- **Solution**: Verify the downloads token has `DOWNLOADS:READ` scope
- **Solution**: Ensure no extra spaces in gradle.properties token values

### Issue: Gradle Sync Failed
- **Check**: Maven repository URL is correct in settings.gradle.kts
- **Check**: Token credentials are properly formatted
- **Try**: Invalidate Caches and Restart (File > Invalidate Caches)

## Key Differences from Google Maps

| Feature | Google Maps | Mapbox |
|---------|------------|---------|
| Coordinates | LatLng(lat, lng) | Point.fromLngLat(lng, lat) |
| Markers | Marker component | CircleAnnotation/SymbolAnnotation |
| Camera | CameraPosition | CameraOptions |
| Map Component | GoogleMap | MapboxMap |
| Styles | Limited built-in | Many built-in + custom styles |

## Map Customization Options

### Change Map Style
In [`JobDetailScreen.kt`](app/src/main/java/com/example/workapp/ui/screens/jobs/JobDetailScreen.kt), update the MapStyle:

```kotlin
MapStyle(style = "mapbox://styles/mapbox/streets-v12") // Current
// Other options:
// "mapbox://styles/mapbox/outdoors-v12"
// "mapbox://styles/mapbox/light-v11"
// "mapbox://styles/mapbox/dark-v11"
// "mapbox://styles/mapbox/satellite-v9"
// "mapbox://styles/mapbox/satellite-streets-v12"
```

### Customize Marker Appearance
Modify the CircleAnnotation properties:

```kotlin
CircleAnnotation(
    point = defaultLocation,
    circleRadiusAsDouble = 12.0,  // Marker size
    circleColorInt = Color.Red.hashCode(),  // Marker color
    circleStrokeWidthAsDouble = 3.0,  // Border width
    circleStrokeColorInt = Color.White.hashCode()  // Border color
)
```

### Add Geocoding (Future Enhancement)
To convert location strings to actual coordinates, you can use the Mapbox Geocoding API:
- Documentation: [https://docs.mapbox.com/api/search/geocoding/](https://docs.mapbox.com/api/search/geocoding/)
- This would replace the default Lima coordinates with actual job locations

## Resources

- [Mapbox Maps SDK for Android Documentation](https://docs.mapbox.com/android/maps/guides/)
- [Mapbox Compose Extension](https://github.com/mapbox/mapbox-maps-android/tree/main/extension-compose)
- [Mapbox Styles](https://docs.mapbox.com/api/maps/styles/)
- [Mapbox Examples](https://docs.mapbox.com/android/maps/examples/)

## Security Notes

⚠️ **Important**: 
- Keep your secret download token (`sk.*`) private and never commit it to version control
- The public token (`pk.*`) can be safely included in your app
- Consider using `.gitignore` to exclude `gradle.properties` or use environment variables for CI/CD
- For production, implement additional token restrictions in your Mapbox account settings

## Next Steps

1. ✅ Set up Mapbox access tokens
2. ✅ Test the map display in job details
3. 🔄 Consider adding geocoding to show actual job locations
4. 🔄 Explore adding custom map styles
5. 🔄 Add additional map features (directions, search, etc.)

---

**Integration completed on**: 2025-11-17  
**Mapbox SDK version**: 11.12.0-beta.1  
**Documentation maintained by**: Development Team