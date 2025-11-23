"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateCraftsmanNames = exports.sendPushNotification = exports.onApplicationStatusChanged = exports.onMessageCreated = exports.onJobApplicationCreated = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
// Initialize Firebase Admin SDK
admin.initializeApp();
/**
 * Send push notification when a new job application is created
 * Triggers when a document is created in the job_applications collection
 */
exports.onJobApplicationCreated = functions.firestore
    .document("job_applications/{applicationId}")
    .onCreate(async (snapshot, context) => {
    var _a, _b, _c;
    try {
        const application = snapshot.data();
        const applicationId = context.params.applicationId;
        // Get job details
        const jobDoc = await admin
            .firestore()
            .collection("jobs")
            .doc(application.jobId)
            .get();
        if (!jobDoc.exists) {
            console.error("Job not found:", application.jobId);
            return;
        }
        const job = jobDoc.data();
        const employerId = job === null || job === void 0 ? void 0 : job.userId;
        if (!employerId) {
            console.error("No employer found for job:", application.jobId);
            return;
        }
        // Get employer's FCM token
        const tokenDoc = await admin
            .firestore()
            .collection("fcm_tokens")
            .doc(employerId)
            .get();
        if (!tokenDoc.exists) {
            console.log("No FCM token found for employer:", employerId);
            return;
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.token;
        if (!token) {
            console.error("Token data is invalid for employer:", employerId);
            return;
        }
        // Get applicant details
        const applicantDoc = await admin
            .firestore()
            .collection("users")
            .doc(application.userId)
            .get();
        const applicantName = applicantDoc.exists
            ? ((_b = applicantDoc.data()) === null || _b === void 0 ? void 0 : _b.name) || "Someone"
            : "Someone";
        // Send notification
        const message = {
            token: token,
            notification: {
                title: "New Job Application",
                body: `${applicantName} applied for ${(job === null || job === void 0 ? void 0 : job.title) || "your job"}`,
            },
            data: {
                action: "JOB_APPLICATION",
                applicationId: applicationId,
                jobId: application.jobId,
                applicantId: application.userId,
                notificationId: Date.now().toString(),
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "job_applications",
                    sound: "default",
                    clickAction: "OPEN_APPLICATION",
                },
            },
            apns: {
                payload: {
                    aps: {
                        sound: "default",
                        badge: 1,
                        category: "JOB_APPLICATION",
                    },
                },
            },
        };
        const response = await admin.messaging().send(message);
        console.log("Successfully sent application notification:", response);
        // Create in-app notification
        await admin.firestore().collection("notifications").add({
            userId: employerId,
            type: "JOB_APPLICATION",
            title: "New Job Application",
            message: `${applicantName} applied for ${(job === null || job === void 0 ? void 0 : job.title) || "your job"}`,
            data: {
                applicationId: applicationId,
                jobId: application.jobId,
                applicantId: application.userId,
            },
            priority: "NORMAL",
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
    }
    catch (error) {
        console.error("Error sending application notification:", error);
        // Clean up invalid tokens
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
            const application = snapshot.data();
            const jobDoc = await admin
                .firestore()
                .collection("jobs")
                .doc(application.jobId)
                .get();
            const employerId = (_c = jobDoc.data()) === null || _c === void 0 ? void 0 : _c.userId;
            if (employerId) {
                await admin
                    .firestore()
                    .collection("fcm_tokens")
                    .doc(employerId)
                    .delete();
                console.log("Deleted invalid token for user:", employerId);
            }
        }
    }
});
/**
 * Send push notification when a new message is created
 * Triggers when a document is created in the messages collection
 */
exports.onMessageCreated = functions.firestore
    .document("messages/{messageId}")
    .onCreate(async (snapshot, context) => {
    var _a, _b, _c, _d;
    try {
        const message = snapshot.data();
        const messageId = context.params.messageId;
        // Get recipient's FCM token
        const recipientId = message.recipientId;
        const senderId = message.senderId;
        if (!recipientId) {
            console.error("No recipient found for message:", messageId);
            return;
        }
        const tokenDoc = await admin
            .firestore()
            .collection("fcm_tokens")
            .doc(recipientId)
            .get();
        if (!tokenDoc.exists) {
            console.log("No FCM token found for recipient:", recipientId);
            return;
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.token;
        if (!token) {
            console.error("Token data is invalid for recipient:", recipientId);
            return;
        }
        // Get sender details
        const senderDoc = await admin
            .firestore()
            .collection("users")
            .doc(senderId)
            .get();
        const senderName = senderDoc.exists
            ? ((_b = senderDoc.data()) === null || _b === void 0 ? void 0 : _b.name) || "Someone"
            : "Someone";
        // Send notification
        const notificationMessage = {
            token: token,
            notification: {
                title: `New message from ${senderName}`,
                body: ((_c = message.text) === null || _c === void 0 ? void 0 : _c.substring(0, 100)) || "Sent you a message",
            },
            data: {
                action: "NEW_MESSAGE",
                messageId: messageId,
                chatId: message.chatId || "",
                senderId: senderId,
                notificationId: Date.now().toString(),
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "messages",
                    sound: "default",
                    clickAction: "OPEN_CHAT",
                },
            },
            apns: {
                payload: {
                    aps: {
                        sound: "default",
                        badge: 1,
                        category: "NEW_MESSAGE",
                    },
                },
            },
        };
        const response = await admin.messaging().send(notificationMessage);
        console.log("Successfully sent message notification:", response);
        // Create in-app notification
        await admin.firestore().collection("notifications").add({
            userId: recipientId,
            type: "NEW_MESSAGE",
            title: `New message from ${senderName}`,
            message: ((_d = message.text) === null || _d === void 0 ? void 0 : _d.substring(0, 100)) || "Sent you a message",
            data: {
                messageId: messageId,
                chatId: message.chatId || "",
                senderId: senderId,
            },
            priority: "HIGH",
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
    }
    catch (error) {
        console.error("Error sending message notification:", error);
        // Clean up invalid tokens
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
            const messageData = snapshot.data();
            const recipientId = messageData.recipientId;
            if (recipientId) {
                await admin
                    .firestore()
                    .collection("fcm_tokens")
                    .doc(recipientId)
                    .delete();
                console.log("Deleted invalid token for user:", recipientId);
            }
        }
    }
});
/**
 * Send push notification when application status changes
 * Triggers when a job_applications document is updated
 */
exports.onApplicationStatusChanged = functions.firestore
    .document("job_applications/{applicationId}")
    .onUpdate(async (change, context) => {
    var _a, _b;
    try {
        const newData = change.after.data();
        const oldData = change.before.data();
        const applicationId = context.params.applicationId;
        // Check if status changed
        if (newData.status === oldData.status) {
            return;
        }
        // Get applicant's FCM token
        const applicantId = newData.userId;
        const tokenDoc = await admin
            .firestore()
            .collection("fcm_tokens")
            .doc(applicantId)
            .get();
        if (!tokenDoc.exists) {
            console.log("No FCM token found for applicant:", applicantId);
            return;
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.token;
        if (!token) {
            console.error("Token data is invalid for applicant:", applicantId);
            return;
        }
        // Get job details
        const jobDoc = await admin
            .firestore()
            .collection("jobs")
            .doc(newData.jobId)
            .get();
        const jobTitle = jobDoc.exists ? (_b = jobDoc.data()) === null || _b === void 0 ? void 0 : _b.title : "the job";
        // Determine notification message based on status
        let title = "";
        let body = "";
        switch (newData.status) {
            case "accepted":
                title = "Application Accepted!";
                body = `Your application for ${jobTitle} has been accepted`;
                break;
            case "rejected":
                title = "Application Update";
                body = `Your application for ${jobTitle} status has changed`;
                break;
            default:
                title = "Application Update";
                body = `Your application for ${jobTitle} has been updated`;
        }
        // Send notification
        const message = {
            token: token,
            notification: {
                title: title,
                body: body,
            },
            data: {
                action: "APPLICATION_STATUS_CHANGED",
                applicationId: applicationId,
                jobId: newData.jobId,
                status: newData.status,
                notificationId: Date.now().toString(),
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "application_updates",
                    sound: "default",
                },
            },
            apns: {
                payload: {
                    aps: {
                        sound: "default",
                        badge: 1,
                    },
                },
            },
        };
        const response = await admin.messaging().send(message);
        console.log("Successfully sent status change notification:", response);
        // Create in-app notification
        await admin.firestore().collection("notifications").add({
            userId: applicantId,
            type: "APPLICATION_STATUS_CHANGED",
            title: title,
            message: body,
            data: {
                applicationId: applicationId,
                jobId: newData.jobId,
                status: newData.status,
            },
            priority: newData.status === "accepted" ? "HIGH" : "NORMAL",
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
    }
    catch (error) {
        console.error("Error sending status change notification:", error);
        // Clean up invalid tokens
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
            const applicationData = change.after.data();
            const applicantId = applicationData.userId;
            if (applicantId) {
                await admin
                    .firestore()
                    .collection("fcm_tokens")
                    .doc(applicantId)
                    .delete();
                console.log("Deleted invalid token for user:", applicantId);
            }
        }
    }
});
/**
 * Callable function to send custom push notification
 * Can be called from the app to send manual notifications
 */
exports.sendPushNotification = functions.https.onCall(async (data, context) => {
    var _a;
    // Verify user is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    }
    const { userId, title, body, dataPayload } = data;
    if (!userId || !title || !body) {
        throw new functions.https.HttpsError("invalid-argument", "Missing required fields: userId, title, body");
    }
    try {
        // Get user's FCM token
        const tokenDoc = await admin
            .firestore()
            .collection("fcm_tokens")
            .doc(userId)
            .get();
        if (!tokenDoc.exists) {
            throw new functions.https.HttpsError("not-found", "No FCM token found for user");
        }
        const token = (_a = tokenDoc.data()) === null || _a === void 0 ? void 0 : _a.token;
        if (!token) {
            throw new functions.https.HttpsError("not-found", "Token data is invalid");
        }
        // Send notification
        const message = {
            token: token,
            notification: {
                title: title,
                body: body,
            },
            data: Object.assign(Object.assign({}, dataPayload), { notificationId: Date.now().toString() }),
            android: {
                priority: "high",
                notification: {
                    channelId: "default",
                    sound: "default",
                },
            },
            apns: {
                payload: {
                    aps: {
                        sound: "default",
                        badge: 1,
                    },
                },
            },
        };
        const response = await admin.messaging().send(message);
        console.log("Successfully sent custom notification:", response);
        return { success: true, messageId: response };
    }
    catch (error) {
        console.error("Error sending custom notification:", error);
        // Clean up invalid tokens
        if (error.code === "messaging/invalid-registration-token" ||
            error.code === "messaging/registration-token-not-registered") {
            await admin.firestore().collection("fcm_tokens").doc(userId).delete();
            console.log("Deleted invalid token for user:", userId);
        }
        throw new functions.https.HttpsError("internal", error.message);
    }
});
/**
 * Callable function to update craftsman names in existing applications
 * This is a one-time migration function
 */
exports.updateCraftsmanNames = functions.https.onCall(async (data, context) => {
    var _a;
    // Verify user is authenticated and is an admin (optional, remove if not needed)
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    }
    try {
        // Get all applications with "Unknown" craftsman name
        const applicationsSnapshot = await admin
            .firestore()
            .collection("job_applications")
            .where("craftsmanName", "==", "Unknown")
            .get();
        console.log(`Found ${applicationsSnapshot.size} applications to update`);
        // Update each application with the correct craftsman name
        const batch = admin.firestore().batch();
        let count = 0;
        for (const doc of applicationsSnapshot.docs) {
            const application = doc.data();
            const craftsmanId = application.craftsmanId;
            // Get craftsman's name from users collection
            const craftsmanDoc = await admin
                .firestore()
                .collection("users")
                .doc(craftsmanId)
                .get();
            if (craftsmanDoc.exists) {
                const craftsmanName = ((_a = craftsmanDoc.data()) === null || _a === void 0 ? void 0 : _a.name) || "Unknown";
                batch.update(doc.ref, { craftsmanName: craftsmanName });
                count++;
                console.log(`Queued update for application ${doc.id}: ${craftsmanName}`);
            }
        }
        await batch.commit();
        console.log(`Successfully updated ${count} applications`);
        return { success: true, updatedCount: count };
    }
    catch (error) {
        console.error("Error updating craftsman names:", error);
        throw new functions.https.HttpsError("internal", error.message);
    }
});
//# sourceMappingURL=index.js.map