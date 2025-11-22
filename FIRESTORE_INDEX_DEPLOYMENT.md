# Firestore Index Deployment Guide

## Current Status ✅

**Good News!** The indexes have been created and are currently building. You're seeing this error:

```
FAILED_PRECONDITION: The query requires an index. That index is currently building and cannot be used yet.
```

This is **expected behavior**. The indexes are being built in the background and will be ready shortly (usually 1-5 minutes, but can take up to 10 minutes for the first index build).

## What to Do Now

**Simply wait 2-5 minutes**, then restart the app. The indexes should be ready by then.

To check index status:
1. Go to: https://console.firebase.google.com/project/workapp-76f52/firestore/indexes
2. Look for indexes in the "jobs" collection
3. Wait until status changes from "Building" → "Enabled"
4. Once all indexes show "Enabled", restart the app

---

## Background: Why This Happened

The app initially crashed with the error:
```
FAILED_PRECONDITION: The query requires an index.
```

This happens because Firestore requires composite indexes for queries that filter and sort on multiple fields.

## Solution

I've updated the [`firestore.indexes.json`](firestore.indexes.json) file with the required indexes for the jobs collection. Now you need to deploy these indexes to Firebase.

### Option 1: Deploy via Firebase Console (Easiest)

Click on the links provided in the error logs to automatically create the indexes:

1. **For clientId + createdAt query:**
   https://console.firebase.google.com/v1/r/project/workapp-76f52/firestore/indexes?create_composite=Ckpwcm9qZWN0cy93b3JrYXBwLTc2ZjUyL2RhdGFiYXNlcy8oZGVmYXVsdCkvY29sbGVjdGlvbkdyb3Vwcy9qb2JzL2luZGV4ZXMvXxABGgwKCGNsaWVudElkEAEaDQoJY3JlYXRlZEF0EAIaDAoIX19uYW1lX18QAg

2. **For status + createdAt query:**
   https://console.firebase.google.com/v1/r/project/workapp-76f52/firestore/indexes?create_composite=Ckpwcm9qZWN0cy93b3JrYXBwLTc2ZjUyL2RhdGFiYXNlcy8oZGVmYXVsdCkvY29sbGVjdGlvbkdyb3Vwcy9qb2JzL2luZGV4ZXMvXxABGgoKBnN0YXR1cxABGg0KCWNyZWF0ZWRBdBACGgwKCF9fbmFtZV9fEAI

These links will open the Firebase Console with the index configuration pre-filled. Just click "Create Index" on each one.

**Index Building Time:** After clicking "Create Index", it typically takes 1-5 minutes for the indexes to build. You'll see the status change from "Building" to "Enabled" in the Firebase Console.

### Option 2: Deploy via Firebase CLI

If you have Firebase CLI installed globally, run:

```bash
firebase deploy --only firestore:indexes
```

If you don't have it installed globally, you can install it:

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:indexes
```

### Option 3: Manual Index Creation

Go to the Firebase Console manually:

1. Open: https://console.firebase.google.com/project/workapp-76f52/firestore/indexes
2. Click "Add Index"
3. Create these three composite indexes:

**Index 1: Jobs by Client**
- Collection: `jobs`
- Fields:
  - `clientId` - Ascending
  - `createdAt` - Descending

**Index 2: Jobs by Status**
- Collection: `jobs`
- Fields:
  - `status` - Ascending
  - `createdAt` - Descending

**Index 3: Jobs by Craftsman**
- Collection: `jobs`
- Fields:
  - `craftsmanId` - Ascending
  - `createdAt` - Descending

## Indexes Configuration

The following indexes have been added to [`firestore.indexes.json`](firestore.indexes.json):

```json
{
  "indexes": [
    {
      "collectionGroup": "jobs",
      "queryScope": "COLLECTION",
      "fields": [
        {"fieldPath": "clientId", "order": "ASCENDING"},
        {"fieldPath": "createdAt", "order": "DESCENDING"}
      ]
    },
    {
      "collectionGroup": "jobs",
      "queryScope": "COLLECTION",
      "fields": [
        {"fieldPath": "status", "order": "ASCENDING"},
        {"fieldPath": "createdAt", "order": "DESCENDING"}
      ]
    },
    {
      "collectionGroup": "jobs",
      "queryScope": "COLLECTION",
      "fields": [
        {"fieldPath": "craftsmanId", "order": "ASCENDING"},
        {"fieldPath": "createdAt", "order": "DESCENDING"}
      ]
    }
  ]
}
```

## Why These Indexes Are Needed

### 1. clientId + createdAt Index
Used by: [`JobRepository.getJobsByClient()`](work-app/app/src/main/java/com/example/workapp/data/repository/JobRepository.kt:38)
```kotlin
firestore.collection("jobs")
    .whereEqualTo("clientId", clientId)
    .orderBy("createdAt", Query.Direction.DESCENDING)
```
This query retrieves all jobs created by a specific client, sorted by creation time.

### 2. status + createdAt Index
Used by: [`JobRepository.getOpenJobs()`](work-app/app/src/main/java/com/example/workapp/data/repository/JobRepository.kt:84)
```kotlin
firestore.collection("jobs")
    .whereEqualTo("status", JobStatus.OPEN.name)
    .orderBy("createdAt", Query.Direction.DESCENDING)
```
This query retrieves all open/available jobs for craftsmen to view.

### 3. craftsmanId + createdAt Index
Used by: [`JobRepository.getJobsByCraftsman()`](work-app/app/src/main/java/com/example/workapp/data/repository/JobRepository.kt:61)
```kotlin
firestore.collection("jobs")
    .whereEqualTo("craftsmanId", craftsmanId)
    .orderBy("createdAt", Query.Direction.DESCENDING)
```
This query retrieves all jobs assigned to a specific craftsman.

## After Index Deployment

Once the indexes are created and enabled:

1. **Wait for indexes to build** (1-5 minutes typically)
2. **Restart the app** on your device/emulator
3. **The app should work without crashes**
4. You should be able to:
   - Create jobs from the Create Job screen
   - View all available jobs in the Jobs List screen
   - See your own jobs in the profile/my jobs section

## Verification

To verify the indexes are working:

1. Go to Firebase Console → Firestore → Indexes
2. Check that all three indexes show status "Enabled" (not "Building")
3. Run the app and navigate to the Jobs List screen
4. No crash should occur

## Troubleshooting

If the app still crashes after deploying indexes:

1. **Check index status**: Make sure all indexes show "Enabled" in Firebase Console
2. **Wait longer**: Sometimes indexes can take 5-10 minutes to build for large datasets
3. **Clear app data**: Uninstall and reinstall the app
4. **Check logs**: Look for any other Firestore errors in Logcat

## Next Steps

After indexes are deployed, you may want to:

1. Add more indexes if you create additional complex queries
2. Monitor index usage in Firebase Console
3. Remove unused indexes to save on storage costs