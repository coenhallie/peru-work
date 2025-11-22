# User ID Mapping Fix

## Problem Identified

The edit and delete buttons weren't showing because of a **field name mismatch** between Firestore and the app.

### Root Cause Analysis (via Firebase MCP)

**Firestore Data Structure:**
```
users/vl6iKwcyaUUKtwczLzLW5OSHfOo1
  _id: "vl6iKwcyaUUKtwczLzLW5OSHfOo1"  ← Field name is "_id"
  email: "coenhallie@gmail.com"
  ...

jobs/T4qTkyxPERLjX5WakO0w
  clientId: "vl6iKwcyaUUKtwczLzLW5OSHfOo1"  ← Correctly using Firebase UID
  ...
```

**The Mismatch:**
- Firestore stores user ID as `_id`
- User model expected field named `id`
- When app loaded user, `_id` wasn't mapped to `id`
- Result: `user.id` was empty string ""
- Comparison: `"" != "vl6iKwcyaUUKtwczLzLW5OSHfOo1"` ❌
- Buttons only show when `currentUserId == job.clientId`

## Solution

Added Firestore field name mapping to [`User.kt`](work-app/app/src/main/java/com/example/workapp/data/model/User.kt):

```kotlin
data class User(
    @get:PropertyName("_id")  // Map Firestore "_id" to model "id"
    @set:PropertyName("_id")
    var id: String = "",
    // ... rest of fields
)
```

This tells Firestore to:
- **Read** `_id` from Firestore → populate `id` in User object
- **Write** `id` from User object → store as `_id` in Firestore

## Result

Now the ownership check works correctly:
```kotlin
// In JobsListScreen
onEdit = if (showMyJobs && currentUserId == job.clientId) {
    { onEditJob(job.id) }  // ✅ Now matches!
} else null
```

## Testing Steps

1. **Uninstall old app** from device
2. **Install new build**: `cd work-app && ./reinstall-debug.sh`
3. **Sign in** with your account
4. **Tap "My Jobs"** in bottom navigation
5. **You should now see** ✏️ edit and 🗑️ delete icons next to YOUR jobs

## Why Reinstall is Required

- Android doesn't hot-reload data model changes
- The fix changes how data is deserialized from Firestore
- Must reinstall to use updated User model with proper field mapping

## Files Modified

- [`User.kt`](work-app/app/src/main/java/com/example/workapp/data/model/User.kt) - Added `@PropertyName` annotations