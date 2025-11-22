# Firestore Index and Data Fix

## Issues Resolved

### 1. Missing Composite Index
**Problem:** The app was showing a `FAILED_PRECONDITION` error because Firestore requires a composite index for queries that filter by one field and order by another.

**Query in Question:**
```kotlin
firestore.collection("users")
    .whereEqualTo("roleString", UserRole.CRAFTSMAN.name)
    .orderBy("rating", Query.Direction.DESCENDING)
```

**Solution:** Created and deployed composite indexes in `firestore.indexes.json`:
- Index 1: `roleString` (ascending) + `rating` (descending)
- Index 2: `roleString` (ascending) + `craft` (ascending) + `rating` (descending)

### 2. Missing Craftsmen Data
**Problem:** The database had no craftsmen data to display.

**Solution:** 
- Updated `seed-craftsmen.js` to populate the `users` collection (instead of `craftsmen`)
- Added `roleString: 'CRAFTSMAN'` field to match the app's query expectations
- Seeded 8 craftsmen with complete profiles

## Changes Made

### Files Created/Modified
1. **firestore.indexes.json** - Composite index configuration
2. **firestore.rules** - Security rules for users and jobs collections
3. **firebase.json** - Firebase project configuration
4. **seed-craftsmen.js** - Updated to use correct collection and field structure

### Deployed
- Firestore indexes
- Firestore security rules

### Seeded Data
Successfully added 8 craftsmen to the database:
- Carlos Mendoza (Carpintería)
- María González (Plomería)
- Jorge Ramírez (Electricidad)
- Ana Torres (Pintura)
- Luis Vargas (Albañilería)
- Patricia Silva (Jardinería)
- Roberto Díaz (Gasfitería)
- Carmen Flores (Limpieza)

## Next Steps

1. **Wait for Index Build:** Firestore composite indexes can take a few minutes to build. You should see the craftsmen data appear in the app once the indexes are ready.

2. **Verify in Firebase Console:** You can check index status at:
   https://console.firebase.google.com/project/workapp-76f52/firestore/indexes

3. **Test the App:** Once indexes are built, log in to the app and you should see the list of craftsmen sorted by rating.

## Index Status
The indexes are currently building. This typically takes 2-5 minutes for small datasets.

You can monitor progress in the Firebase Console under:
**Firestore Database > Indexes**