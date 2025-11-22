# Mapbox Integration Setup Guide

## Overview

The Work App uses Mapbox to display interactive maps showing job locations. This feature is **optional** - the app will work without it, displaying location information as text instead.

## Current Status

⚠️ **Maps are currently disabled** because no Mapbox access token is configured.

The app will display job locations as text-only cards instead of interactive maps. To enable map functionality, follow the setup instructions below.

## Why Mapbox?

- **Interactive Maps**: Display job locations on an interactive map
- **Location Visualization**: Help craftsmen see exactly where jobs are located
- **Professional UI**: Enhance the user experience with beautiful map interfaces

## Setup Instructions

### Step 1: Get a Mapbox Access Token

1. Go to [Mapbox](https://www.mapbox.com/) and create a free account
2. Navigate to your [Account Dashboard](https://account.mapbox.com/)
3. Go to the **Access Tokens** section
4. Click **Create a token** or use your default public token
5. Copy your access token (it starts with `pk.`)

**Note**: Mapbox offers a generous free tier with 50,000 free map loads per month.

### Step 2: Configure the Token

Create or edit the `gradle.properties` file in the project root:

```properties
# Mapbox Configuration
MAPBOX_PUBLIC_TOKEN=pk.your_actual_token_here
```

**Important**: 
- Replace `pk.your_actual_token_here` with your actual Mapbox token
- Never commit this file to version control (it's already in `.gitignore`)

### Step 3: Add to strings.xml (Alternative Method)

Alternatively, you can add the token to `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <!-- Other strings -->
    <string name="mapbox_access_token">pk.your_actual_token_here</string>
</resources>
```

### Step 4: Rebuild the App

After adding your token:

```bash
cd work-app
./gradlew clean build
```

Or in Android Studio:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

## Verification

After setup, when you click on a job listing:

✅ **With Token**: You'll see an interactive map with a marker showing the job location
❌ **Without Token**: You'll see a simple card displaying the location as text

## Troubleshooting

### App Still Crashes on Job Details

If you're still experiencing crashes:

1. **Verify Token Format**: Ensure your token starts with `pk.`
2. **Clean Build**: Run `./gradlew clean` and rebuild
3. **Check gradle.properties**: Ensure no extra spaces or quotes around the token
4. **Restart Android Studio**: Sometimes a restart is needed to pick up new build config

### Maps Not Displaying

If maps don't show but the app doesn't crash:

1. **Check Internet Connection**: Maps require an active internet connection
2. **Token Validity**: Verify your token is active in your Mapbox dashboard
3. **Token Restrictions**: Ensure your token doesn't have URL restrictions that block the app

## Token Security

### Best Practices

✅ **DO**:
- Use public tokens (starting with `pk.`) for client-side apps
- Store tokens in `gradle.properties` (already gitignored)
- Rotate tokens periodically
- Use token restrictions in Mapbox dashboard for production

❌ **DON'T**:
- Commit tokens to version control
- Share tokens publicly
- Use secret tokens in client apps

### Production Considerations

For production releases:

1. **Token Restrictions**: In Mapbox dashboard, restrict your token to your app's package name
2. **Separate Tokens**: Use different tokens for development and production
3. **Monitor Usage**: Keep an eye on your Mapbox usage in the dashboard

## Alternative: Running Without Maps

The app is fully functional without Mapbox. If you prefer not to set up maps:

- Job locations will be displayed as text in a card
- All other functionality remains intact
- No crashes or errors will occur

## Additional Resources

- [Mapbox Android SDK Documentation](https://docs.mapbox.com/android/maps/guides/)
- [Mapbox Compose Extension](https://github.com/mapbox/mapbox-maps-android/tree/main/extension-compose)
- [Mapbox Pricing](https://www.mapbox.com/pricing)

## Support

If you encounter issues:

1. Check the [Mapbox Community Forum](https://community.mapbox.com/)
2. Review the [Android SDK Troubleshooting Guide](https://docs.mapbox.com/android/maps/guides/troubleshooting/)
3. Verify your token is active in the Mapbox dashboard

---

**Last Updated**: November 2024