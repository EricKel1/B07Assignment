const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const { setGlobalOptions } = require("firebase-functions/v2");

admin.initializeApp();

// Set global options (optional)
setGlobalOptions({ maxInstances: 10 });

/**
 * Triggers when a new notification is added to the 'notifications' collection.
 * Sends an FCM Push Notification to the target user.
 */
exports.sendPushNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
      const snap = event.data;
      if (!snap) {
          console.log("No data associated with the event");
          return;
      }
      const notification = snap.data();
      const userId = notification.userId; // The target user ID (Parent)
      const title = notification.title;
      const message = notification.message;

      if (!userId || !title || !message) {
        console.log("Invalid notification data");
        return;
      }

      try {
        // 1. Get the user's FCM token from their profile
        const userDoc = await admin.firestore().collection("users").doc(userId).get();
        
        if (!userDoc.exists) {
          console.log(`User ${userId} not found`);
          return;
        }

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) {
          console.log(`No FCM token found for user ${userId}`);
          return;
        }

        // 2. Construct the FCM V1 Message
        const payload = {
          token: fcmToken,
          notification: {
            title: title,
            body: message,
          },
          data: {
            click_action: "FLUTTER_NOTIFICATION_CLICK", // Standard click action
            notificationId: event.params.notificationId,
          },
        };

        // 3. Send the message using the Admin SDK (uses V1 API automatically)
        const response = await admin.messaging().send(payload);
        console.log("Successfully sent message:", response);
        
      } catch (error) {
        console.error("Error sending push notification:", error);
      }
    });
