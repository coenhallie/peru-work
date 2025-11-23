const admin = require('firebase-admin');
const serviceAccount = require('./google-services.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function updateCraftsmanNames() {
  try {
    // Get the craftsman's actual name
    const craftsmanDoc = await db.collection('users').doc('J2xG77LiYLdcgqJA8Kd97ilkyAu1').get();
    const craftsmanName = craftsmanDoc.data()?.name || 'Unknown';
    
    console.log(`Updating applications to craftsman name: ${craftsmanName}`);
    
    // Get all applications with "Unknown" craftsman name
    const applicationsSnapshot = await db.collection('job_applications')
      .where('craftsmanName', '==', 'Unknown')
      .get();
    
    console.log(`Found ${applicationsSnapshot.size} applications to update`);
    
    // Update each application
    const batch = db.batch();
    let count = 0;
    
    applicationsSnapshot.forEach((doc) => {
      batch.update(doc.ref, { craftsmanName: craftsmanName });
      count++;
      console.log(`Queued update for application: ${doc.id}`);
    });
    
    await batch.commit();
    console.log(`Successfully updated ${count} applications`);
    
  } catch (error) {
    console.error('Error updating applications:', error);
    process.exit(1);
  }
  
  process.exit(0);
}

updateCraftsmanNames();