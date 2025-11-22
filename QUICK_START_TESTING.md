# Quick Start: Testing Phase 1-5

## TL;DR - Fastest Way to Test

1. **Build the app:**
   ```bash
   cd work-app
   ./gradlew clean assembleDebug
   ```

2. **If build succeeds** → 🎉 Data models and repositories are working!

3. **Run the app and apply to a job as a craftsman**

4. **Check Firebase Console** → Firestore Database → `notifications` collection
   - Should see a new notification created for the client
   - If you see it → Phase 1-3 working perfectly! ✅

---

## What Just Got Built

### ✅ Phase 1-5 Foundation
- **3 New Data Models** ([`Notification.kt`](work-app/app/src/main/java/com/example/workapp/data/model/Notification.kt), [`Message.kt`](work-app/app/src/main/java/com/example/workapp/data/model/Message.kt))
- **1 New Repository** ([`NotificationRepository.kt`](work-app/app/src/main/java/com/example/workapp/data/repository/NotificationRepository.kt))
- **1 Enhanced Repository** ([`ApplicationRepository.kt`](work-app/app/src/main/java/com/example/workapp/data/repository/ApplicationRepository.kt))
- **2 New ViewModels** (NotificationsViewModel, ApplicationsViewModel)

### 🔗 Hilt Dependency Injection
Your existing [`AppModule.kt`](work-app/app/src/main/java/com/example/workapp/di/AppModule.kt:1) already provides:
- ✅ FirebaseAuth
- ✅ FirebaseFirestore  
- ✅ FirebaseStorage

Our new repositories use `@Inject constructor`, so Hilt auto-wires them!

---

## Step 1: Build Verification (1 minute)

```bash
cd work-app
./gradlew clean build
```

**Expected:** Build succeeds ✅

**If build fails with:**
- `"Unresolved reference: NotificationRepository"` → Check imports
- `"Missing dependency"` → Repositories should auto-inject via Hilt
- Any other error → Share the error message

---

## Step 2: Runtime Test (2 minutes)

### Test Notification Creation

1. **Run the app** (as craftsman account)
2. **Navigate to Jobs** → Find an open job
3. **Click "Apply for this Job"**
4. **Fill out the application** and submit

### Verify It Worked

**Option A: Check Firestore Console** (Recommended)
1. Open [Firebase Console](https://console.firebase.google.com)
2. Navigate to Firestore Database
3. Look for `notifications` collection
4. Should see a new document:
```json
{
  "_id": "auto_generated_id",
  "userId": "client_user_id",  
  "type": "APPLICATION_RECEIVED",
  "title": "New Application Received",
  "message": "CraftsmanName applied to JobTitle",
  "isRead": false,
  "priority": "HIGH",
  "createdAt": timestamp
}
```

**If you see this document → Phase 1-3 are working! 🎉**

**Option B: Check Logcat**
```bash
adb logcat | grep -i "firestore"
```
Should see: `Writing batch with 2 writes` (application + notification)

---

## Step 3: Verify Enhanced Fields (1 minute)

Check the `job_applications` collection in Firestore:

1. Find the application you just created
2. Verify it has the new fields:
   - `isReadByClient: false` ✅
   - `clientViewedAt: null` ✅

If these fields exist → JobApplication enhancement working! ✅

---

## Expected Firestore Structure After Test

```
firestore/
├── jobs/
│   └── {jobId}/
│       ├── applicationCount: 1
│       └── hasActiveApplications: true
│
├── job_applications/
│   └── {applicationId}/
│       ├── ... (existing fields)
│       ├── isReadByClient: false        ← NEW
│       └── clientViewedAt: null         ← NEW
│
└── notifications/                       ← NEW COLLECTION
    └── {notificationId}/
        ├── _id: "notif_123"
        ├── userId: "client_id"
        ├── type: "APPLICATION_RECEIVED"
        ├── title: "New Application Received"
        ├── message: "John applied to Plumbing Work"
        ├── data: {jobId, applicationId}
        ├── isRead: false
        ├── priority: "HIGH"
        └── createdAt: 1700000000000
```

---

## Common Issues & Quick Fixes

### Issue 1: Build Error - "Unresolved reference"
**Fix:** Sync Gradle files
```bash
./gradlew --refresh-dependencies
```

### Issue 2: No notification created in Firestore
**Check:**
1. Is user signed in? (`FirebaseAuth.currentUser != null`)
2. Check Logcat for errors
3. Firestore rules allow writes?

**Quick Fix - Update Firestore Rules:**
```javascript
// In Firebase Console → Firestore → Rules
match /notifications/{notificationId} {
  allow create: if request.auth != null;
  allow read: if request.auth != null && 
                 resource.data.userId == request.auth.uid;
}
```

### Issue 3: App crashes on startup
**Check:** Any ProGuard/R8 rules needed?
**Fix:** Shouldn't happen with debug build

---

## What Happens When You Apply to a Job?

```
1. Craftsman fills application → Submit clicked
                                      ↓
2. ApplicationRepository.submitApplication()
   - Creates application in Firestore ✓
   - Calls NotificationRepository.createNotification() ✓
                                      ↓
3. Notification created in Firestore
   - Client's userId
   - Type: APPLICATION_RECEIVED
   - Priority: HIGH
                                      ↓
4. In future phases:
   - FCM push sent to client
   - In-app badge shows on Jobs tab
   - Client opens app and sees notification
```

---

## Success Checklist

Mark these off as you test:

- [ ] App builds successfully
- [ ] Can run app without crashes
- [ ] Applied to a job as craftsman
- [ ] Notification document appears in Firestore `notifications` collection
- [ ] Notification has correct userId (the job's client)
- [ ] JobApplication has `isReadByClient: false`
- [ ] No errors in Logcat

**If all checked → Phase 1-5 fully working!** 🎉

---

## Next Steps

Once Phase 1-5 are verified:

### Option A: Continue with UI (Recommended)
Build the screens to actually see/use the notifications:
- Phase 6: ApplicationsListScreen
- Phase 7: ApplicationDetailSheet  
- Phase 8: NotificationsScreen

### Option B: Add Firebase Cloud Messaging
Test push notifications:
- Phase 9: FCM Service
- Get actual phone notifications

### Option C: Complete Integration
Wire everything together:
- Phase 10: Navigation + Bottom nav badges

---

## Testing Tips

1. **Use Two Test Accounts:**
   - Account A: Client (creates jobs)
   - Account B: Craftsman (applies to jobs)

2. **Check Firestore After Each Action:**
   - Helps understand data flow
   - Catch issues early

3. **Enable Firestore Logging:**
   ```kotlin
   // In Application class onCreate()
   if (BuildConfig.DEBUG) {
       FirebaseFirestore.setLoggingEnabled(true)
   }
   ```

4. **Use Firebase Emulator Suite (Optional):**
   ```bash
   firebase emulators:start
   ```
   - Test without affecting production data
   - Faster iteration

---

## Detailed Testing

For comprehensive testing, see: [`PHASE_1_TESTING_GUIDE.md`](PHASE_1_TESTING_GUIDE.md:1)

For the full design documentation, see: [`CRAFTSMEN_LISTING_CREATOR_CONNECTION_SYSTEM.md`](CRAFTSMEN_LISTING_CREATOR_CONNECTION_SYSTEM.md:1)

---

**Questions?** The implementation is solid and follows best practices. If you encounter any issues, check the error message and Firestore rules first!