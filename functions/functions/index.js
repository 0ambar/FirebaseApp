const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// HTTP version of the notification function (easier to debug)
exports.sendNotificationHttp = functions.https.onRequest(async (req, res) => {
  try {
    // CORS headers
    res.set('Access-Control-Allow-Origin', '*');
    res.set('Access-Control-Allow-Methods', 'GET, POST');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    
    // Handle preflight OPTIONS request
    if (req.method === 'OPTIONS') {
      res.status(204).send('');
      return;
    }
    
    // Ensure method is POST
    if (req.method !== 'POST') {
      res.status(405).send('Method Not Allowed');
      return;
    }
    
    // Get request body
    const { title, message, token } = req.body;
    
    if (!title || !message || !token) {
      res.status(400).send('Missing required parameters: title, message, token');
      return;
    }
    
    console.log(`Attempting to send notification with token: ${token}`);
    
    const payload = {
      notification: {
        title,
        body: message,
      },
      data: {
        title,
        message,
        click_action: "FLUTTER_NOTIFICATION_CLICK",
      },
    };
    
    try {
      // Comment this out temporarily to test if the function is working
      // await admin.messaging().sendToDevice(token, payload);
      res.status(200).json({ success: true, message: "Test mode - notification not actually sent" });
    } catch (error) {
      console.error("Error sending message:", error);
      res.status(500).json({ error: error.message });
    }
  } catch (error) {
    console.error("Unexpected error:", error);
    res.status(500).json({ error: error.message });
  }
});

// Original callable function (simplified)
exports.sendNotification = functions.https.onCall(async (data, context) => {
  console.log("Called sendNotification with data:", JSON.stringify(data));
  
  // Skip authentication temporarily for testing
  // if (!context.auth) {
  //   throw new functions.https.HttpsError(
  //     "unauthenticated",
  //     "El usuario debe estar autenticado"
  //   );
  // }

  const { title, message, token } = data;

  if (!title || !message || !token) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Se requieren título, mensaje y token"
    );
  }

  console.log("Attempting to send to token:", token);
  
  return { success: true, message: "Function executed (test mode - no actual message sent)" };
});

// Función para enviar notificación a un tema
exports.sendNotificationToTopic = functions.https.onCall(async (data, context) => {
  console.log("Called sendNotificationToTopic with data:", JSON.stringify(data));
  
  // Skip authentication temporarily for testing
  // if (!context.auth) {
  //   throw new functions.https.HttpsError(
  //     "unauthenticated",
  //     "El usuario debe estar autenticado"
  //   );
  // }

  const { title, message, topic } = data;

  if (!title || !message || !topic) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Se requieren título, mensaje y tema"
    );
  }
  const payload = {
    notification: {
      title,
      body: message,
    },
    data: {
      title,
      message,
      click_action: "FLUTTER_NOTIFICATION_CLICK",
    },
  };

  try {
    console.log("Would have sent notification to topic:", topic);
    // Temporarily disabled to test function execution
    // await admin.messaging().sendToTopic(topic, payload);
    return { success: true, message: "Test mode - no actual message sent" };
  } catch (error) {
    console.error("Error sending to topic:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// Función que se ejecuta cuando se crea una nueva notificación en Firestore
exports.onNewNotification = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const notificationData = snapshot.data();
    const { title, message, receiverUid } = notificationData;

    console.log("New notification in Firestore:", JSON.stringify(notificationData));
    console.log("Notification ID:", context.params.notificationId);

    // Obtener el token FCM del destinatario
    try {
      const userDoc = await admin.firestore().collection("users").doc(receiverUid).get();
      
      if (!userDoc.exists) {
        console.log("Usuario no encontrado:", receiverUid);
        return { success: false, error: "User not found" };
      }
      
      const userData = userDoc.data();
      const token = userData.fcmToken;
      
      if (!token) {
        console.log("Token no disponible para el usuario:", receiverUid);
        return { success: false, error: "FCM token not available" };
      }
      
      console.log("Would send notification to token:", token);
      
      // For testing, just return success without trying to send
      return { success: true, message: "Test mode - notification logged but not sent" };
    } catch (error) {
      console.error("Error al enviar notificación:", error);
    }
  });