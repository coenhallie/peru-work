# Convex Database Setup - Peru Work Project

This document describes the Convex backend setup for the Peru Work Android application.

## Overview

The application now uses Convex as its backend database instead of local mock data. All craftsmen and job data is stored in and retrieved from Convex in real-time.

## Architecture

### Backend (convex-backend/)

**Deployment URL:** https://adjoining-hamster-879.convex.cloud
**Dashboard:** https://dashboard.convex.dev/d/adjoining-hamster-879

#### Database Schema

1. **craftsmen** table:
   - `_id`: Auto-generated ID
   - `name`: String
   - `craft`: String
   - `rating`: Number (double)
   - `profileImageUrl`: String
   - `experience`: Int64
   - `description`: String
   - `bio`: String
   - `location`: String
   - `phone`: String
   - `email`: String
   - `specialties`: Array of strings
   - `completedProjects`: Int64
   - `reviewCount`: Int64

2. **jobs** table:
   - `_id`: Auto-generated ID
   - `title`: String
   - `description`: String
   - `location`: String
   - `craftsmanId`: ID reference to craftsmen table

#### Available Functions

**Craftsmen Queries:**
- `craftsmen:list` - Get all craftsmen
- `craftsmen:getById` - Get a specific craftsman by ID
- `craftsmen:getByCraft` - Filter craftsmen by craft type

**Jobs Queries:**
- `jobs:list` - Get all jobs
- `jobs:getById` - Get a specific job by ID
- `jobs:getByCraftsmanId` - Get jobs for a specific craftsman

**Seed Functions:**
- `seed:seedCraftsmen` - Populate craftsmen data
- `seed:seedJobs` - Populate jobs data

### Android App Integration

#### Dependencies Added

```kotlin
// Convex Android SDK
implementation("dev.convex:android-convexmobile:0.4.0@aar") {
    isTransitive = true
}
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
```

#### Key Components

1. **ConvexClientProvider** (`data/ConvexClientProvider.kt`)
   - Singleton that provides the Convex client instance
   - Connects to the deployment URL

2. **CraftsmenRepository** (`data/CraftsmenRepository.kt`)
   - Repository pattern for data access
   - Provides Flow-based reactive queries

3. **Updated Data Models**
   - Added `@Serializable` annotations
   - Changed types to match Convex schema (Long for int64, Double for float64)
   - Added `@ConvexNum` for numeric fields
   - Added `@SerialName("_id")` for ID fields

4. **Updated Navigation**
   - Now uses CraftsmenRepository instead of MockData
   - Reactive updates via Flow and collectAsState

## Current Data

**6 Craftsmen:**
1. John Doe - Plumber
2. Jane Smith - Painter
3. Peter Jones - Gardener
4. Maria Garcia - Electrician
5. David Wilson - Carpenter
6. Sarah Brown - Mason

**3 Jobs:**
1. Leaky Faucet Repair (John Doe)
2. Living Room Painting (Jane Smith)
3. Garden Weeding (Peter Jones)

## Development

### Running the Convex Backend

```bash
cd convex-backend
npx convex dev
```

This starts the development server which:
- Watches for changes in the `convex/` directory
- Auto-deploys functions to your deployment
- Provides real-time logs

### Re-seeding Data

To reset and re-populate the database:

```bash
# Using Convex dashboard or CLI
npx convex run seed:seedCraftsmen
npx convex run seed:seedJobs
```

### Adding New Functions

1. Create a new `.ts` file in `convex-backend/convex/`
2. Export query, mutation, or action functions
3. The dev server will auto-deploy

Example:
```typescript
import { query } from "./_generated/server";

export const myNewQuery = query({
  args: {},
  handler: async (ctx) => {
    return await ctx.db.query("craftsmen").collect();
  },
});
```

### Modifying Schema

Edit `convex-backend/convex/schema.ts` and the changes will be deployed automatically by the dev server.

## Android App Usage

The app now automatically:
- Subscribes to real-time data from Convex
- Updates the UI when data changes
- Handles loading and error states

### Example: Getting All Craftsmen

```kotlin
val repository = CraftsmenRepository()
val craftsmenFlow = repository.getAllCraftsmen()

// In Compose
val craftsmenResult by craftsmenFlow.collectAsState(initial = Result.success(emptyList()))
val craftsmen = craftsmenResult.getOrElse { emptyList() }
```

## Permissions

The AndroidManifest.xml already includes the required INTERNET permission:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Next Steps

Potential enhancements:
1. Add authentication with Convex Auth
2. Implement real-time job status updates
3. Add search and filtering capabilities
4. Implement pagination for large datasets
5. Add offline support with local caching
6. Create mutation functions for CRUD operations

## Troubleshooting

**Issue:** App shows no data
- Verify Convex dev server is running
- Check internet connection
- Verify deployment URL in ConvexClientProvider.kt

**Issue:** Build errors
- Run `./gradlew clean build`
- Sync Gradle files
- Verify all dependencies are downloaded

**Issue:** TypeScript errors in backend
- Run `npm install` in convex-backend/
- Check Convex CLI version: `npx convex --version`

## Resources

- [Convex Documentation](https://docs.convex.dev/)
- [Convex Android SDK](https://github.com/get-convex/convex-mobile)
- [Dashboard](https://dashboard.convex.dev/d/adjoining-hamster-879)