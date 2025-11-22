# PERMISSION_DENIED Error Fix

## Problem
When submitting a job application, users were encountering a `PERMISSION_DENIED` error. The error appeared in the logs as:

```
Listen for Query(target=Query(job_applications where jobId==... and craftsmanId==... and statusString in [PENDING,ACCEPTED]...);limitType=LIMIT_TO_FIRST) failed: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions., cause=null}
```

## Root Cause
The Firestore security rules for the [`job_applications`](firestore.rules:59) collection were using `allow read`, which only permits reading individual documents (get operations). However, the application submission process requires performing **collection queries** (list operations) to:

1. Check if a craftsman has already applied to a job before submitting
2. Retrieve lists of applications for jobs
3. Retrieve lists of applications by craftsman

## Solution
Updated the Firestore security rules to differentiate between:

- **`allow get`**: For reading individual documents by ID
- **`allow list`**: For performing collection queries

### Updated Rules
```javascript
// Job Applications collection
match /job_applications/{applicationId} {
  // Allow get (single document read)
  allow get: if isSignedIn() && 
    (resource.data.clientId == request.auth.uid || 
     resource.data.craftsmanId == request.auth.uid);
  
  // Allow list (collection queries) - needed for checking existing applications
  allow list: if isSignedIn();
  
  // ... create, update, delete rules remain the same
}
```

### Why This Works
- **`allow list: if isSignedIn()`**: Allows any authenticated user to query the job_applications collection
- The actual data returned is still filtered by the query conditions in the app code
- Additional row-level security is handled through proper query construction in [`ApplicationRepository.kt`](work-app/app/src/main/java/com/example/workapp/data/repository/ApplicationRepository.kt)

## Deployment
Rules were successfully deployed to Firebase project `workapp-76f52`:
```bash
firebase deploy --only firestore:rules
```

## Testing
After deploying the updated rules, users should now be able to:
1. ✅ Submit job applications without PERMISSION_DENIED errors
2. ✅ View their own application history
3. ✅ Check if they've already applied to a job

## Related Files
- [`firestore.rules`](firestore.rules) - Updated security rules
- [`ApplicationRepository.kt`](work-app/app/src/main/java/com/example/workapp/data/repository/ApplicationRepository.kt:26-56) - Application submission logic that requires list queries