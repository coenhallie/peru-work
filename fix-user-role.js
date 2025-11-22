const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'workapp-76f52'
});

const db = admin.firestore();

async function fixUserRole() {
  try {
    const userId = 'h0HR5idqlXW0THBD5eEau1RywaE2';
    const userRef = db.collection('users').doc(userId);
    
    // Get current document
    const doc = await userRef.get();
    if (!doc.exists) {
      console.error('User not found!');
      process.exit(1);
    }
    
    const userData = doc.data();
    console.log('Current user data:', JSON.stringify(userData, null, 2));
    
    // Update the document - add roleString field and remove incorrect role field
    await userRef.update({
      roleString: 'CRAFTSMAN',
      role: admin.firestore.FieldValue.delete() // Remove the incorrect field
    });
    
    console.log('\n✅ Successfully updated user role field!');
    
    // Verify the update
    const updatedDoc = await userRef.get();
    const updatedData = updatedDoc.data();
    console.log('\nUpdated user data:', JSON.stringify(updatedData, null, 2));
    
    process.exit(0);
  } catch (error) {
    console.error('Error fixing user role:', error);
    process.exit(1);
  }
}

fixUserRole();