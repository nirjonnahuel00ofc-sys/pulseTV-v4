package com.example.config

import android.content.Context
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * ============================================================================
 *  PulseTV Firebase Configuration & Setup Guide (Zero Re-Install Remote Sync)
 * ============================================================================
 *
 * How to connect PulseTV to your own fresh Firebase project:
 * 1. Replace `app/google-services.json` with your new Firebase Android app's
 *    `google-services.json` (package: `com.aistudio.pulsetv.krmxvp`).
 * 2. Update `R.string.firestore_database_id` in
 *    `app/src/main/res/values/firebase_applet_config.xml` (or set
 *    CUSTOM_DATABASE_ID_OVERRIDE below if using a non-default named database).
 * 3. In `firestore.rules`, update `isAdmin()` with your Google Sign-In email
 *    or add your UID under `/admins/{uid}` in the Firebase Console, then deploy:
 *      - Public Read: `channels`, `live_events`, `updates_inbox`
 *      - Validated Viewer Create: `broken_reports`
 *      - Admin-Only Write/Manage: `channels`, `live_events`, `updates_inbox`, `broken_reports`
 * 4. Optional: Change DEFAULT_ADMIN_PIN below before building your release APK.
 */
object FirebaseConfig {

    /**
     * Leave blank ("") to automatically load the provisioned database ID from
     * `res/values/firebase_applet_config.xml` (`R.string.firestore_database_id`).
     * Paste your custom Firestore database ID here if pointing to another DB.
     */
    private const val CUSTOM_DATABASE_ID_OVERRIDE = ""

    /**
     * Secret PIN to unlock the hidden Admin Panel (long-press the
     * PulseTV logo in the top bar). Never displayed in the UI.
     */
    const val DEFAULT_ADMIN_PIN = "2110636243"

    /**
     * Firestore collection names (must match `firestore.rules` & `firebase-blueprint.json`).
     */
    const val COLLECTION_CHANNELS = "channels"
    const val COLLECTION_BANNERS = "banners"
    const val COLLECTION_LIVE_EVENTS = "live_events"
    const val COLLECTION_UPDATES_INBOX = "updates_inbox"
    const val COLLECTION_BROKEN_REPORTS = "broken_reports"
    const val COLLECTION_APP_METRICS = "app_metrics"
    const val DOC_ANALYTICS_SUMMARY = "summary"

    const val CONTACT_EMAIL = "pulsetv.bdofc65@gmail.com"
    const val CONTACT_TELEGRAM_DISPLAY = "t.me/pulsetv_dl_ofc"
    const val CONTACT_TELEGRAM_URL = "https://t.me/pulsetv_dl_ofc"

    /**
     * Returns the configured [FirebaseFirestore] instance with offline disk cache
     * enabled for fast startup on low-end devices and slower networks.
     */
    fun getFirestore(context: Context): FirebaseFirestore {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        val databaseId = CUSTOM_DATABASE_ID_OVERRIDE.ifBlank {
            appContext.getString(R.string.firestore_database_id)
        }
        val db = FirebaseFirestore.getInstance(databaseId)
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
        } catch (_: IllegalStateException) {
            // Firestore instance already started; keep active settings.
        }
        return db
    }
}
