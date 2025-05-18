# Firebase App - README

## Autenticación de Usuarios

La autenticación de usuarios en esta aplicación se gestiona mediante Firebase Authentication. Permite a los usuarios registrarse e iniciar sesión utilizando su dirección de correo electrónico y contraseña.

### Flujo de Registro (`RegisterActivity.kt`)

1.  **Entrada de Datos**: El usuario proporciona su nombre, correo electrónico, contraseña y la confirmación de la contraseña.
    *   Opcionalmente, puede marcarse como administrador si conoce la contraseña maestra de administrador (definida en `Constants.ADMIN_MASTER_PASSWORD`).
2.  **Validaciones**:
    *   Se verifica que todos los campos estén completos.
    *   Se comprueba que las contraseñas coincidan.
    *   Si se marca como administrador, se valida la contraseña de administrador.
3.  **Creación de Usuario en Firebase Auth**:
    *   Se llama a `auth.createUserWithEmailAndPassword(email, password)`.
4.  **Obtención de Token FCM**:
    *   Tras el registro exitoso, se obtiene el token de Firebase Cloud Messaging (FCM) del dispositivo mediante `FirebaseMessaging.getInstance().token`.
5.  **Almacenamiento en Firestore**:
    *   Se crea un objeto `User` que incluye:
        *   `uid` (del usuario de Firebase Auth).
        *   `email`.
        *   `name`.
        *   `admin` (booleano).
        *   `fcmToken` (el token FCM obtenido).
        *   `createdAt` (timestamp actual).
    *   Este objeto `User` se guarda en la colección `users` de Firestore, utilizando el `uid` del usuario como ID del documento.
6.  **Redirección**:
    *   Si el registro es exitoso y el usuario es administrador, se redirige a `AdminDashboardActivity.kt`.
    *   Si es un usuario normal, se redirige a `MainActivity.kt`.

### Flujo de Inicio de Sesión (`LoginActivity.kt`)

1.  **Entrada de Datos**: El usuario proporciona su correo electrónico y contraseña.
2.  **Validaciones**: Se verifica que ambos campos estén completos.
3.  **Inicio de Sesión con Firebase Auth**:
    *   Se llama a `auth.signInWithEmailAndPassword(email, password)`.
4.  **Actualización de Token FCM**:
    *   Tras el inicio de sesión exitoso, se obtiene el token FCM más reciente del dispositivo.
    *   Se actualiza el campo `fcmToken` en el documento del usuario en la colección `users` de Firestore. Esto asegura que las notificaciones se envíen al dispositivo correcto si el token ha cambiado.
5.  **Obtención de Datos del Usuario**:
    *   Se recupera el documento del usuario desde la colección `users` de Firestore utilizando el `uid`.
6.  **Redirección**:
    *   Si el usuario es administrador (`user.admin == true`), se redirige a `AdminDashboardActivity.kt`.
    *   Si es un usuario normal, se redirige a `MainActivity.kt`.

### Gestión de Sesión

*   Firebase Auth maneja automáticamente la persistencia de la sesión del usuario.
*   En `MainActivity.onStart()` y `AdminDashboardActivity.onStart()`, se verifica si `auth.currentUser` es nulo. Si lo es, significa que no hay un usuario autenticado, y la app redirige a `LoginActivity.kt`.
*   El cierre de sesión se realiza mediante `auth.signOut()`, lo que limpia la sesión actual y redirige al usuario a `LoginActivity.kt`.

### Modelo de Usuario (`User.kt`)

El modelo de datos para un usuario (`com.example.firebaseapp.models.User`) contiene:

*   `uid`: String - Identificador único del usuario (proviene de Firebase Auth).
*   `email`: String - Correo electrónico del usuario.
*   `name`: String - Nombre del usuario.
*   `admin`: Boolean - Indica si el usuario tiene privilegios de administrador.
*   `createdAt`: Long - Timestamp de cuándo se creó el usuario.
*   `fcmToken`: String - Token de Firebase Cloud Messaging para enviar notificaciones push al dispositivo del usuario.

## Sistema de Notificaciones

El sistema de notificaciones permite enviar mensajes push a los dispositivos de los usuarios. Se basa en Firebase Cloud Messaging (FCM) y Firebase Functions.

### Componentes Principales

1.  **Aplicación Android**:
    *   **`FirebaseMessagingService.kt`**:
        *   Servicio que se ejecuta en segundo plano para recibir mensajes FCM.
        *   `onMessageReceived()`: Se invoca cuando la app recibe un mensaje FCM (ya sea en primer o segundo plano si es un mensaje de datos).
            *   Extrae el título y el mensaje del payload de datos de la notificación.
            *   Utiliza `NotificationHelper.kt` para mostrar la notificación en el dispositivo.
        *   `onNewToken()`: Se invoca cuando FCM genera un nuevo token de registro para el dispositivo.
            *   Actualiza el token en Firestore para el usuario autenticado actual.
            *   Guarda el token en SharedPreferences para acceso local.
            *   Resuscribe al tema "all" con el nuevo token.
    *   **`NotificationHelper.kt`**:
        *   Clase de utilidad para crear y mostrar notificaciones en el sistema Android.
        *   `showNotification()`: Construye y muestra una notificación utilizando `NotificationCompat.Builder`. Configura el título, mensaje, ícono, sonido, vibración, canal de notificación (para Android 8.0+), y un `PendingIntent` para abrir `MainActivity.kt` cuando se toca la notificación.
        *   `createNotificationChannel()`: Crea un canal de notificación (requerido para Android 8.0 Oreo y superior).
        *   `subscribeToTopics()`: Suscribe la app al tema "all" de FCM, permitiendo enviar notificaciones a todos los usuarios.
    *   **`FirebaseApplication.kt`**:
        *   Clase `Application` personalizada.
        *   En `onCreate()`:
            *   Inicializa Firebase.
            *   Crea el canal de notificación.
            *   Suscribe a temas FCM mediante `NotificationHelper`.
            *   Obtiene y actualiza el token FCM en Firestore para el usuario autenticado.
    *   **`NotificationHistoryActivity.kt`**:
        *   Muestra un historial de las notificaciones recibidas por el usuario.
        *   Carga las notificaciones desde la colección `notifications` en Firestore, filtrando por el `receiverUid` del usuario actual y ordenándolas por `timestamp`.
    *   **Permisos**:
        *   La app solicita el permiso `POST_NOTIFICATIONS` en Android 13 (API 33) y superior en `MainActivity.kt`.
    *   **Manejo de Notificaciones al Abrir la App**:
        *   `MainActivity.handleNotificationIntent()`: Verifica si la `Activity` fue iniciada por un clic en una notificación y puede mostrar un `Toast` o realizar otras acciones.

2.  **Firebase Cloud Functions (`functions/functions/index.js`)**:
    *   **`onNewNotification` (Trigger de Firestore)**:
        *   Se activa automáticamente cuando se crea un nuevo documento en la colección `notifications/{notificationId}` de Firestore.
        *   **Lógica**:
            1.  Obtiene los datos de la notificación recién creada (`title`, `message`, `receiverUid`).
            2.  Busca en la colección `users` el documento del usuario cuyo `uid` coincide con `receiverUid`.
            3.  Extrae el `fcmToken` del documento del usuario.
            4.  Si se encuentra un token válido:
                *   Construye un payload de mensaje FCM que incluye:
                    *   `notification`: Contiene `title` y `body`.
                    *   `data`: Puede incluir datos adicionales como `title`, `message`, `click_action`.
                    *   `token`: El `fcmToken` del destinatario.
                *   Envía el mensaje utilizando `admin.messaging().send(payload)`.
            5.  Registra el éxito o el fracaso del envío.

### Flujo de Envío de Notificaciones (Ejemplo: Administrador envía notificación)

1.  **Creación de Notificación en la App (Lado del Administrador - `SendNotificationActivity.kt`)**:
    *   Un administrador redacta un título y un mensaje.
    *   Puede elegir enviar a usuarios específicos o a todos los usuarios (mediante un tema o iterando).
    *   **Envío a Usuario Específico**:
        1.  Se guarda un nuevo documento en la colección `notifications` de Firestore. Este documento contiene:
            *   `id`: ID único de la notificación.
            *   `title`: Título de la notificación.
            *   `message`: Cuerpo del mensaje.
            *   `senderUid`: UID del administrador que envía.
            *   `senderName`: Nombre del administrador.
            *   `receiverUid`: UID del usuario destinatario.
            *   `timestamp`: Hora de creación.
            *   `read`: Booleano, inicialmente `false`.
        2.  La creación de este documento en Firestore dispara la Cloud Function `onNewNotification`.
    *   **Envío a Todos los Usuarios (vía Cloud Function `sendNotificationToTopic`)**:
        1.  La app llama a la Cloud Function `sendNotificationToTopic` (una función HTTPS Callable).
        2.  La Cloud Function `sendNotificationToTopic` envía un mensaje FCM al tema "all" usando `admin.messaging().sendToTopic("all", payload)`.
        3.  Adicionalmente, `SendNotificationActivity` también crea registros individuales en la colección `notifications` para cada usuario, lo que también activará `onNewNotification` para cada uno si se desea un registro individual y la posibilidad de que la función `onNewNotification` maneje el envío (aunque esto podría ser redundante si `sendNotificationToTopic` ya envió la notificación push). *Nota: La implementación actual en `SendNotificationActivity.kt` para "todos los usuarios" parece enfocarse en llamar a `sendNotificationToTopic` y luego guardar registros en Firestore, lo que podría llevar a que `onNewNotification` también intente enviar.*

2.  **Procesamiento por Cloud Function (`onNewNotification`)**:
    *   Como se describió anteriormente, la función se activa, obtiene el token FCM del destinatario y envía el mensaje push.

3.  **Recepción en el Dispositivo**:
    *   El servicio `FirebaseMessagingService.kt` en la app del usuario recibe el mensaje.
    *   `onMessageReceived()` se ejecuta.
    *   `NotificationHelper.kt` muestra la notificación en la barra de estado del dispositivo.
    *   Si el usuario toca la notificación, se abre `MainActivity.kt`.

### Almacenamiento de Notificaciones

*   Todas las notificaciones enviadas (o al menos sus metadatos) se almacenan en la colección `notifications` de Firestore.
*   Esto permite a los usuarios ver su historial de notificaciones en `NotificationHistoryActivity.kt`.

### Tokens FCM

*   El token FCM es crucial para dirigir las notificaciones a un dispositivo específico.
*   La app se encarga de:
    *   Obtener el token FCM al inicio (`FirebaseApplication.kt`).
    *   Obtener el token FCM al registrar un nuevo usuario (`RegisterActivity.kt`).
    *   Actualizar el token FCM en Firestore cuando un usuario inicia sesión (`LoginActivity.kt`) o cuando se genera un nuevo token (`FirebaseMessagingService.onNewToken()`).
    *   Almacenar el `fcmToken` en el documento de cada usuario en la colección `users` de Firestore.
