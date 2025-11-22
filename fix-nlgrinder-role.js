const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixNlgrinderRole() {
  try {
    console.log('Fixing nlgrinder@live.nl user role...');
    
    const userId = 'SKJS01PV6TT2yaj7V3X02SGz0o82';
    const userRef = db.collection('users').doc(userId);
    
    // Get current data
    const doc = await userRef.get();
    if (!doc.exists) {
      console.error('User not found!');
      return;
    }
    
    console.log('Current data:', doc.data());
    
    // Update to craftsman role with proper field name
    await userRef.update({
      roleString: 'CRAFTSMAN',
      craft: 'plumber', // Add craft field
      // Remove old 'role' field if it exists
      role: admin.firestore.FieldValue.delete()
    });
    
    console.log('✅ Successfully updated nlgrinder@live.nl to CRAFTSMAN role');
    
    // Verify the update
    const updatedDoc = await userRef.get();
    console.log('Updated data:', updatedDoc.data());
    
  } catch (error) {
    console.error('Error fixing user role:', error);
  } finally {
    process.exit(0);
  }
}

fixNlgrinderRole();