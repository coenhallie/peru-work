
import * as admin from 'firebase-admin';

// Initialize Firebase Admin
// Try to initialize with default credentials
try {
    admin.initializeApp();
} catch (e) {
    console.log("App already initialized or error", e);
}

const db = admin.firestore();

async function migrateUsers() {
    console.log("Migrating users...");
    const snapshot = await db.collection('users').where('roleString', '==', 'CRAFTSMAN').get();
    console.log(`Found ${snapshot.size} users to migrate.`);

    const batch = db.batch();
    let count = 0;

    snapshot.docs.forEach(doc => {
        const data = doc.data();
        const updateData: any = {
            roleString: 'PROFESSIONAL',
            profession: data.craft || null
        };

        // Delete old field
        updateData.craft = admin.firestore.FieldValue.delete();

        batch.update(doc.ref, updateData);
        count++;
    });

    if (count > 0) {
        await batch.commit();
        console.log(`Migrated ${count} users.`);
    } else {
        console.log("No users to migrate.");
    }
}

async function migrateJobs() {
    console.log("Migrating jobs...");
    // We can't easily query for non-null fields in all cases, so we'll fetch all jobs that might have a craftsman
    // Or just fetch all jobs since dataset is small (checked < 50)
    const snapshot = await db.collection('jobs').get();
    console.log(`Found ${snapshot.size} jobs to check.`);

    const batch = db.batch();
    let count = 0;

    snapshot.docs.forEach(doc => {
        const data = doc.data();
        if (data.craftsmanId || data.craftsmanName) {
            const updateData: any = {
                professionalId: data.craftsmanId || null,
                professionalName: data.craftsmanName || null,
                craftsmanId: admin.firestore.FieldValue.delete(),
                craftsmanName: admin.firestore.FieldValue.delete()
            };
            batch.update(doc.ref, updateData);
            count++;
        }
    });

    if (count > 0) {
        await batch.commit();
        console.log(`Migrated ${count} jobs.`);
    } else {
        console.log("No jobs to migrate.");
    }
}

async function migrateApplications() {
    console.log("Migrating applications...");
    const snapshot = await db.collection('job_applications').get();
    console.log(`Found ${snapshot.size} applications to check.`);

    const batch = db.batch();
    let count = 0;

    snapshot.docs.forEach(doc => {
        const data = doc.data();
        // Check if migration is needed (if old fields exist)
        if (data.craftsmanId !== undefined) {
            const updateData: any = {
                professionalId: data.craftsmanId || null,
                professionalName: data.craftsmanName || null,
                professionalProfileImage: data.craftsmanProfileImage || null,
                professionalRating: data.craftsmanRating || null,
                professionalExperience: data.craftsmanExperience || null,
                professionalProfession: data.craftsmanCraft || null, // Note: mapping craft -> profession

                // Delete old fields
                craftsmanId: admin.firestore.FieldValue.delete(),
                craftsmanName: admin.firestore.FieldValue.delete(),
                craftsmanProfileImage: admin.firestore.FieldValue.delete(),
                craftsmanRating: admin.firestore.FieldValue.delete(),
                craftsmanExperience: admin.firestore.FieldValue.delete(),
                craftsmanCraft: admin.firestore.FieldValue.delete()
            };
            batch.update(doc.ref, updateData);
            count++;
        }
    });

    if (count > 0) {
        await batch.commit();
        console.log(`Migrated ${count} applications.`);
    } else {
        console.log("No applications to migrate.");
    }
}

async function run() {
    try {
        await migrateUsers();
        await migrateJobs();
        await migrateApplications();
        console.log("Migration completed successfully.");
    } catch (error) {
        console.error("Migration failed:", error);
    }
}

run();
