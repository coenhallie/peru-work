import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Send push notification when a new job application is created
 * Triggers when a document is created in the job_applications collection
 */
export const onJobApplicationCreated = functions.firestore
  .document("job_applications/{applicationId}")
  .onCreate(async (snapshot, context) => {
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
      const employerId = job?.userId;

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

      const token = tokenDoc.data()?.token;

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
        ? applicantDoc.data()?.name || "Someone"
        : "Someone";

      // Send notification
      const message = {
        token: token,
        notification: {
          title: "New Job Application",
          body: `${applicantName} applied for ${job?.title || "your job"}`,
        },
        data: {
          action: "JOB_APPLICATION",
          applicationId: applicationId,
          jobId: application.jobId,
          applicantId: application.userId,
          notificationId: Date.now().toString(),
        },
        android: {
          priority: "high" as const,
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
        message: `${applicantName} applied for ${job?.title || "your job"}`,
        data: {
          applicationId: applicationId,
          jobId: application.jobId,
          applicantId: application.userId,
        },
        priority: "NORMAL",
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (error: any) {
      console.error("Error sending application notification:", error);

      // Clean up invalid tokens
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        const application = snapshot.data();
        const jobDoc = await admin
          .firestore()
          .collection("jobs")
          .doc(application.jobId)
          .get();
        const employerId = jobDoc.data()?.userId;

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
 * Send push notification when a new chat room is created (service request)
 * Triggers when a document is created in the chat_rooms collection
 * This notifies the professional that a client wants to start a conversation
 */
export const onChatRoomCreated = functions.firestore
  .document("chat_rooms/{chatRoomId}")
  .onCreate(async (snapshot, context) => {
    try {
      const chatRoom = snapshot.data();
      const chatRoomId = context.params.chatRoomId;

      // Only send notification for direct messages (service requests)
      // These have empty jobId and start with "direct_"
      if (!chatRoomId.startsWith("direct_")) {
        console.log("Not a direct chat room, skipping notification");
        return;
      }

      const professionalId = chatRoom.professionalId;
      const clientName = chatRoom.clientName || "A client";

      if (!professionalId) {
        console.error("No professional found for chat room:", chatRoomId);
        return;
      }

      // Get professional's FCM token
      const tokenDoc = await admin
        .firestore()
        .collection("fcm_tokens")
        .doc(professionalId)
        .get();

      if (!tokenDoc.exists) {
        console.log("No FCM token found for professional:", professionalId);
        // Still create in-app notification even without FCM token
      } else {
        const token = tokenDoc.data()?.token;

        if (token) {
          // Send push notification
          const message = {
            token: token,
            notification: {
              title: "New Service Request",
              body: `${clientName} wants to discuss a service with you`,
            },
            data: {
              action: "NEW_CHAT",
              chatRoomId: chatRoomId,
              clientId: chatRoom.clientId || "",
              clientName: clientName,
              notificationId: Date.now().toString(),
            },
            android: {
              priority: "high" as const,
              notification: {
                channelId: "service_requests",
                sound: "default",
                clickAction: "OPEN_CHAT",
              },
            },
            apns: {
              payload: {
                aps: {
                  sound: "default",
                  badge: 1,
                  category: "NEW_CHAT",
                },
              },
            },
          };

          try {
            const response = await admin.messaging().send(message);
            console.log(
              "Successfully sent service request notification:",
              response
            );
          } catch (error: any) {
            console.error("Error sending push notification:", error);
            // Clean up invalid tokens
            if (
              error.code === "messaging/invalid-registration-token" ||
              error.code === "messaging/registration-token-not-registered"
            ) {
              await admin
                .firestore()
                .collection("fcm_tokens")
                .doc(professionalId)
                .delete();
              console.log("Deleted invalid token for user:", professionalId);
            }
          }
        }
      }

      // Create in-app notification
      await admin.firestore().collection("notifications").add({
        userId: professionalId,
        type: "NEW_CHAT",
        title: "New Service Request",
        message: `${clientName} wants to discuss a service with you`,
        data: {
          chatRoomId: chatRoomId,
          clientId: chatRoom.clientId || "",
          clientName: clientName,
        },
        actionUrl: `chat/${chatRoomId}`,
        imageUrl: chatRoom.clientProfileImage || null,
        priority: "HIGH",
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log("Created in-app notification for service request");
    } catch (error: any) {
      console.error("Error in onChatRoomCreated:", error);
    }
  });

/**
 * Send push notification when a new message is created in a chat room
 * Triggers when a document is created in the chat_rooms/{chatRoomId}/messages subcollection
 */
export const onChatMessageCreated = functions.firestore
  .document("chat_rooms/{chatRoomId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    try {
      const message = snapshot.data();
      const chatRoomId = context.params.chatRoomId;
      const messageId = context.params.messageId;

      const senderId = message.senderId;
      const senderRole = message.senderRole;

      // Get the chat room to find the recipient
      const chatRoomDoc = await admin
        .firestore()
        .collection("chat_rooms")
        .doc(chatRoomId)
        .get();

      if (!chatRoomDoc.exists) {
        console.error("Chat room not found:", chatRoomId);
        return;
      }

      const chatRoom = chatRoomDoc.data();

      // Determine recipient based on sender role
      let recipientId: string;
      if (senderRole === "CLIENT") {
        recipientId = chatRoom?.professionalId || chatRoom?.craftsmanId;
      } else {
        recipientId = chatRoom?.clientId;
      }

      if (!recipientId) {
        console.error("No recipient found for message:", messageId);
        return;
      }

      // Get recipient's FCM token
      const tokenDoc = await admin
        .firestore()
        .collection("fcm_tokens")
        .doc(recipientId)
        .get();

      if (!tokenDoc.exists) {
        console.log("No FCM token found for recipient:", recipientId);
        return;
      }

      const token = tokenDoc.data()?.token;

      if (!token) {
        console.error("Token data is invalid for recipient:", recipientId);
        return;
      }

      // Send notification
      const senderName = message.senderName || "Someone";
      const messageText =
        message.type === "IMAGE"
          ? "Sent you an image"
          : message.message?.substring(0, 100) || "Sent you a message";

      const notificationMessage = {
        token: token,
        notification: {
          title: `New message from ${senderName}`,
          body: messageText,
        },
        data: {
          action: "NEW_MESSAGE",
          messageId: messageId,
          chatRoomId: chatRoomId,
          senderId: senderId,
          notificationId: Date.now().toString(),
        },
        android: {
          priority: "high" as const,
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
      console.log("Successfully sent chat message notification:", response);

      // Create in-app notification
      await admin.firestore().collection("notifications").add({
        userId: recipientId,
        type: "NEW_MESSAGE",
        title: `New message from ${senderName}`,
        message: messageText,
        data: {
          messageId: messageId,
          chatRoomId: chatRoomId,
          senderId: senderId,
        },
        actionUrl: `chat/${chatRoomId}`,
        priority: "NORMAL",
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (error: any) {
      console.error("Error sending chat message notification:", error);

      // Clean up invalid tokens
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        const chatRoomDoc = await admin
          .firestore()
          .collection("chat_rooms")
          .doc(context.params.chatRoomId)
          .get();

        const chatRoom = chatRoomDoc.data();
        const messageData = snapshot.data();
        const recipientId =
          messageData.senderRole === "CLIENT"
            ? chatRoom?.professionalId
            : chatRoom?.clientId;

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
 * Send push notification when a new message is created
 * Triggers when a document is created in the messages collection
 * @deprecated Use onChatMessageCreated for chat_rooms/{chatRoomId}/messages subcollection
 */
export const onMessageCreated = functions.firestore
  .document("messages/{messageId}")
  .onCreate(async (snapshot, context) => {
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

      const token = tokenDoc.data()?.token;

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
        ? senderDoc.data()?.name || "Someone"
        : "Someone";

      // Send notification
      const notificationMessage = {
        token: token,
        notification: {
          title: `New message from ${senderName}`,
          body: message.text?.substring(0, 100) || "Sent you a message",
        },
        data: {
          action: "NEW_MESSAGE",
          messageId: messageId,
          chatId: message.chatId || "",
          senderId: senderId,
          notificationId: Date.now().toString(),
        },
        android: {
          priority: "high" as const,
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
        message: message.text?.substring(0, 100) || "Sent you a message",
        data: {
          messageId: messageId,
          chatId: message.chatId || "",
          senderId: senderId,
        },
        priority: "HIGH",
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (error: any) {
      console.error("Error sending message notification:", error);

      // Clean up invalid tokens
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
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
export const onApplicationStatusChanged = functions.firestore
  .document("job_applications/{applicationId}")
  .onUpdate(async (change, context) => {
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

      const token = tokenDoc.data()?.token;

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

      const jobTitle = jobDoc.exists ? jobDoc.data()?.title : "the job";

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
          priority: "high" as const,
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
    } catch (error: any) {
      console.error("Error sending status change notification:", error);

      // Clean up invalid tokens
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
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
export const sendPushNotification = functions.https.onCall(
  async (data, context) => {
    // Verify user is authenticated
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { userId, title, body, dataPayload } = data;

    if (!userId || !title || !body) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing required fields: userId, title, body"
      );
    }

    try {
      // Get user's FCM token
      const tokenDoc = await admin
        .firestore()
        .collection("fcm_tokens")
        .doc(userId)
        .get();

      if (!tokenDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "No FCM token found for user"
        );
      }

      const token = tokenDoc.data()?.token;

      if (!token) {
        throw new functions.https.HttpsError(
          "not-found",
          "Token data is invalid"
        );
      }

      // Send notification
      const message = {
        token: token,
        notification: {
          title: title,
          body: body,
        },
        data: {
          ...dataPayload,
          notificationId: Date.now().toString(),
        },
        android: {
          priority: "high" as const,
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
    } catch (error: any) {
      console.error("Error sending custom notification:", error);

      // Clean up invalid tokens
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        await admin.firestore().collection("fcm_tokens").doc(userId).delete();
        console.log("Deleted invalid token for user:", userId);
      }

      throw new functions.https.HttpsError("internal", error.message);
    }
  }
);
