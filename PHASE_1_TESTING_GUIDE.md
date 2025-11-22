# Phase 1 Testing Guide: Data Models & Repository Layer

## Quick Testing Overview

Since Phase 1-5 created the foundation (data models, repositories, ViewModels), you can test them by:
1. Building the app to verify compilation
2. Testing notification creation through existing application flow
3. Verifying Firestore data structure
4. Using Firebase Console to monitor real-time data

---

## Step 1: Build Verification

First, ensure the app compiles with the new dependencies:

```bash
cd work-app
./gradlew clean build
```

**Expected Result:** Build should succeed with no errors.

**Common Issues:**
- If you get dependency errors, the NotificationRepository won't inject properly - we'll fix this in Phase 10
- For now, comment out the `notificationRepository` parameter in ApplicationRepository if needed

---

## Step 2: Test Data Models (Firestore Serialization)

The data models need to correctly serialize to/from Firestore. Test this manually:

### A. Test Notification Model

1. **Open Firebase Console** → Firestore Database
2. **Manually create a test document** in `notifications` collection:

```json
{
  "_id": "test_notif_001",
  "userId": "your_test_user_id",
  "type": "APPLICATION_RECEIVED",
  "title": "Test Notification",
  "message": "This is a test message",
  "data": {
    "jobId": "job123",
    "applicationId": "app456"
  },
  "isRead": false,
  "createdAt": 1700000000000,
  "actionUrl": "applications/job123",
  "priority": "HIGH"
}
```

3. **Run the app** and check logs to see if deserialization works

### B. Test Enhanced JobApplication Model

The existing application submission flow should still work:

1. **Run the app** as a craftsman
2. **Apply to a job** (this already exists in your app)
3. **Check Firestore** → `job_applications` collection
4. **Verify new fields exist:**
   - `isReadByClient: false` ✓
   - `clientViewedAt: null` ✓

**If these fields are missing**, the model didn't serialize correctly.

---

## Step 3: Test NotificationRepository

Create a simple test in your app to verify notifications work:

### Option A: Add Temporary Test Button (Quick Test)

Add this to any screen temporarily:

```kotlin
// In any Composable screen
Button(onClick = {
    // Test notification creation
    viewLifecycleScope.launch {
        val repo = NotificationRepository(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
        
        val result = repo.createNotification(
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            type = NotificationType.APPLICATION_RECEIVED,
            title = "Test Notification",
            message = "Testing notification system",
            data = mapOf("test" to "data"),
            priority = NotificationPriority.HIGH
        )
        
        result.fold(
            onSuccess = { Log.d("TEST", "Notification created: $it") },
            onFailure = { Log.e("TEST", "Failed: ${it.message}") }
        )
    }
}) {
    Text("Test Notification")
}
```

### Option B: Use Existing Application Flow

The better approach - use what's already working:

1. **Sign in as Craftsman**
2. **Apply to a job** using the existing application dialog
3. **Check Firestore Console** immediately:
   - Look for new document in `notifications` collection
   - Should have the client's userId
   - Type should be "APPLICATION_RECEIVED"

**Expected Firestore Structure:**
```
notifications/
  └── auto_generated_id/
      ├── _id: "auto_generated_id"
      ├── userId: "client_user_id"
      ├── type: "APPLICATION_RECEIVED"
      ├── title: "New Application Received"
      ├── message: "John Doe applied to Plumbing Work"
      ├── data: {jobId, applicationId, craftsmanId}
      ├── isRead: false
      ├── createdAt: timestamp
      ├── actionUrl: "applications/jobId"
      ├── imageUrl: "craftsman_profile_url"
      └── priority: "HIGH"
```

---

## Step 4: Verify Repository Integration

Test that ApplicationRepository creates notifications:

### Test Flow:
```
Craftsman applies → ApplicationRepository.submitApplication() 
                 → NotificationRepository.createNotification()
                 → Firestore notification created
```

**Testing Steps:**

1. **Enable Firestore Debug Logging:**
```kotlin
// In your Application class or MainActivity
FirebaseFirestore.setLoggingEnabled(true)
```

2. **Monitor Logcat:**
```bash
adb logcat | grep -i firestore
```

3. **Apply to a job as craftsman**

4. **Watch for Firestore writes:**
```
I/Firestore: (24.5.0) [WriteStream]: Writing batch with 1 writes
I/Firestore: Collection written: job_applications
I/Firestore: Collection written: notifications
```

---

## Step 5: Test Data Model Enums

Verify enum string conversion works:

### Test NotificationType:
```kotlin
// In any test or temporary code
val type = NotificationType.APPLICATION_RECEIVED
println(type.name) // Should print: "APPLICATION_RECEIVED"

val typeFromString = NotificationType.valueOf("APPLICATION_RECEIVED")
println(typeFromString) // Should print: APPLICATION_RECEIVED
```

### Test in Firestore:
1. Create notification with type "APPLICATION_RECEIVED"
2. Read it back
3. Check that `notification.notificationType` returns the enum correctly

---

## Step 6: Integration Test Checklist

| Test | Expected Result | Status |
|------|----------------|--------|
| App compiles | No build errors | ⬜ |
| Notification model serializes | Data in Firestore matches model | ⬜ |
| JobApplication enhanced fields | `isReadByClient` exists in Firestore | ⬜ |
| Message/ChatRoom models compile | No syntax errors | ⬜ |
| NotificationRepository creates notification | Document appears in Firestore | ⬜ |
| Apply to job creates notification | Client receives notification in DB | ⬜ |
| Accept application creates notifications | Winner + losers get notifications | ⬜ |
| Reject application creates notification | Craftsman gets notification | ⬜ |

---

## Step 7: Firebase Console Verification

### Check Firestore Collections:

1. **Open Firebase Console** → Firestore Database

2. **Verify Collections Exist:**
   ```
   ✓ job_applications (existing)
   ✓ notifications (new)
   ✓ fcm_tokens (will create when FCM tested)
   ```

3. **Check Indexes Needed:**
   
   The app will fail queries without these indexes. Firebase will show errors in console suggesting index creation.

   **Notifications Indexes Needed:**
   ```
   Collection: notifications
   Fields indexed:
   - userId (Ascending) + createdAt (Descending)
   - userId (Ascending) + isRead (Ascending)
   ```

   Click the link in error message to auto-create these indexes.

---

## Step 8: Common Issues & Solutions

### Issue 1: "NotificationRepository not found"
**Cause:** Hilt hasn't wired up the dependency yet
**Solution:** We'll fix this in Phase 10 when we update the DI module

**Temporary Fix:**
```kotlin
// Comment out notificationRepository in ApplicationRepository constructor
// Re-enable after Phase 10
```

### Issue 2: Notification not appearing in Firestore
**Check:**
1. User is authenticated (`FirebaseAuth.currentUser != null`)
2. Firestore rules allow writes to `notifications` collection
3. Check Logcat for permission errors

**Fix Firestore Rules:**
```javascript
// firestore.rules
match /notifications/{notificationId} {
  allow create: if request.auth != null;
  allow read: if request.auth != null && 
                 resource.data.userId == request.auth.uid;
  allow update: if request.auth != null && 
                   resource.data.userId == request.auth.uid;
}
```

### Issue 3: "Field doesn't exist" errors
**Cause:** Old documents don't have new fields
**Solution:** 
- Delete test data in Firestore
- Re-submit application
- New documents will have all fields

### Issue 4: Enum conversion fails
**Cause:** String in Firestore doesn't match enum value
**Solution:** Ensure Firestore stores exact enum name:
```kotlin
put("type", type.name) // Correct
// NOT: put("type", type.toString())
```

---

## Step 9: Manual End-to-End Test

**Complete User Flow Test:**

1. **Setup:** 
   - Have 2 test accounts (1 client, 1 craftsman)
   - Create a job as client

2. **As Craftsman:**
   - Sign in
   - Find job in home screen
   - Click job → Apply
   - Fill application form
   - Submit

3. **Verify in Firebase Console:**
   - Check `job_applications` → new application exists
   - Check `notifications` → new notification for client exists
   - Notification data matches application

4. **As Client (simulated):**
   - Check Firestore for notification with your userId
   - Should see: "NewCraftsmanName applied to JobTitle"

5. **Accept Application (future test):**
   - Will create notifications for winner and losers
   - Test this after Phase 6-7 when UI is ready

---

## Step 10: Verify Real-time Updates

Test that ViewModels receive real-time updates:

### Test NotificationsViewModel:
```kotlin
// Add temporary logging in NotificationsViewModel
init {
    viewModelScope.launch {
        notifications.collect { notifs ->
            Log.d("NotificationsVM", "Received ${notifs.size} notifications")
            notifs.forEach {
                Log.d("NotificationsVM", "- ${it.title}: ${it.message}")
            }
        }
    }
}
```

**Test:**
1. Run app with logging enabled
2. Create notification in Firebase Console manually
3. Watch logcat for real-time update
4. Should see: "Received 1 notifications"

---

## Quick Verification Script

Run this in your app to verify all models work:

```kotlin
// Add to MainActivity or test screen
private fun testPhase1() {
    lifecycleScope.launch {
        try {
            // Test 1: Notification serialization
            val notification = Notification(
                id = "test1",
                userId = "user123",
                type = NotificationType.APPLICATION_RECEIVED.name,
                title = "Test",
                message = "Testing",
                priority = NotificationPriority.HIGH.name
            )
            val map = notification.toMap()
            Log.d("TEST", "✓ Notification serialization works: $map")
            
            // Test 2: Enum conversion
            val typeEnum = notification.notificationType
            assert(typeEnum == NotificationType.APPLICATION_RECEIVED)
            Log.d("TEST", "✓ Enum conversion works: $typeEnum")
            
            // Test 3: Message model
            val message = Message(
                id = "msg1",
                chatRoomId = "room1",
                senderId = "user1",
                message = "Hello"
            )
            Log.d("TEST", "✓ Message model works: ${message.toMap()}")
            
            Log.d("TEST", "✅ Phase 1 models verified!")
            
        } catch (e: Exception) {
            Log.e("TEST", "❌ Phase 1 test failed: ${e.message}")
        }
    }
}
```

---

## Success Criteria

Phase 1 is successfully tested when:

✅ App builds without errors  
✅ Application submission still works  
✅ Notification document created in Firestore  
✅ Notification has correct structure  
✅ New JobApplication fields (`isReadByClient`) appear  
✅ Enum conversions work (type string ↔ NotificationType)  
✅ Real-time listeners receive updates  

---

## Next: Testing Phase 2-5

Once Phase 1 passes, test Phase 2-5:

**Phase 2-3:** Repository methods (tested via Phase 1 flow)  
**Phase 4-5:** ViewModels (tested when UI is built in Phase 6-8)

The full integration test will happen in **Phase 10** when everything is wired together!

---

**Need Help?** Check logs for specific errors and verify:
1. Firebase initialized correctly
2. User authenticated
3. Firestore rules allow operations
4. Indexes created for queries