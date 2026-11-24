package com.example.data

import android.util.Log
import com.example.config.FirebaseConfig
import com.example.model.AppAnalyticsSummary
import com.example.model.BannerNotice
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BannerAndAnalyticsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireAdminSession(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in with an authorized Google account to perform admin actions.")
    }

    fun observeBanners(): Flow<List<BannerNotice>> =
        db.collection(FirebaseConfig.COLLECTION_BANNERS)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toBannerNotice() }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(
                    firestoreError ?: error,
                    OperationType.LIST,
                    FirebaseConfig.COLLECTION_BANNERS,
                )
                throw error
            }

    suspend fun createBanner(
        title: String,
        message: String,
        imageData: String,
        actionUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val docRef = db.collection(FirebaseConfig.COLLECTION_BANNERS).document()
        return try {
            requireAdminSession()
            val cleanTitle = validateRequiredText(title, "Banner title", 100)
            val cleanMessage = message.trim().take(400)
            val cleanImage = validateImageData(imageData)
            val cleanAction = validateOptionalHttpUrl(actionUrl, "Action URL", 1000)

            val payload = mapOf(
                "title" to cleanTitle,
                "message" to cleanMessage,
                "image_data" to cleanImage,
                "action_url" to cleanAction,
                "is_active" to isActive,
                "created_at" to FieldValue.serverTimestamp(),
                "updated_at" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateBanner(
        bannerId: String,
        title: String,
        message: String,
        imageData: String,
        actionUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_BANNERS}/$bannerId"
        return try {
            requireAdminSession()
            require(bannerId.isNotBlank()) { "Banner ID cannot be blank" }
            val cleanTitle = validateRequiredText(title, "Banner title", 100)
            val cleanMessage = message.trim().take(400)
            val cleanImage = validateImageData(imageData)
            val cleanAction = validateOptionalHttpUrl(actionUrl, "Action URL", 1000)

            val updates = mapOf(
                "title" to cleanTitle,
                "message" to cleanMessage,
                "image_data" to cleanImage,
                "action_url" to cleanAction,
                "is_active" to isActive,
                "updated_at" to FieldValue.serverTimestamp(),
            )
            db.collection(FirebaseConfig.COLLECTION_BANNERS).document(bannerId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleBannerActive(bannerId: String, isActive: Boolean): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_BANNERS}/$bannerId"
        return try {
            requireAdminSession()
            require(bannerId.isNotBlank()) { "Banner ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_BANNERS).document(bannerId)
                .update(
                    mapOf(
                        "is_active" to isActive,
                        "updated_at" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_BANNERS}/$bannerId"
        return try {
            requireAdminSession()
            require(bannerId.isNotBlank()) { "Banner ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_BANNERS).document(bannerId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAppAnalytics(): Flow<AppAnalyticsSummary> =
        db.collection(FirebaseConfig.COLLECTION_APP_METRICS)
            .document(FirebaseConfig.DOC_ANALYTICS_SUMMARY)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    AppAnalyticsSummary()
                } else {
                    AppAnalyticsSummary(
                        appOpens = snapshot.getLong("app_opens") ?: 0L,
                        channelPlays = snapshot.getLong("channel_plays") ?: 0L,
                        eventPlays = snapshot.getLong("event_plays") ?: 0L,
                        updatedAt = snapshot.getTimestamp("updated_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                    )
                }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(
                        firestoreError ?: error,
                        OperationType.GET,
                        "${FirebaseConfig.COLLECTION_APP_METRICS}/${FirebaseConfig.DOC_ANALYTICS_SUMMARY}",
                    )
                }
                throw error
            }

    suspend fun incrementAppOpen() = incrementMetricField("app_opens")

    suspend fun incrementChannelPlay() = incrementMetricField("channel_plays")

    suspend fun incrementEventPlay() = incrementMetricField("event_plays")

    private suspend fun incrementMetricField(fieldName: String) {
        val docRef = db.collection(FirebaseConfig.COLLECTION_APP_METRICS)
            .document(FirebaseConfig.DOC_ANALYTICS_SUMMARY)
        try {
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    tx.set(
                        docRef,
                        mapOf(
                            "app_opens" to if (fieldName == "app_opens") 1L else 0L,
                            "channel_plays" to if (fieldName == "channel_plays") 1L else 0L,
                            "event_plays" to if (fieldName == "event_plays") 1L else 0L,
                            "updated_at" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    tx.update(
                        docRef,
                        mapOf(
                            fieldName to FieldValue.increment(1L),
                            "updated_at" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                null
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Non-blocking telemetry: log without interrupting playback or guest browsing
            Log.w(TAG, "Analytics metric increment skipped ($fieldName): ${e.localizedMessage}")
        }
    }

    private fun validateRequiredText(raw: String, label: String, maxLen: Int): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "$label is required" }
        require(trimmed.length <= maxLen) { "$label must be $maxLen characters or fewer" }
        return trimmed
    }

    private fun validateImageData(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.length >= 10) { "Please select or paste a banner image" }
        require(trimmed.length <= 450_000) { "Banner image exceeds size limit" }
        val isHttp = trimmed.startsWith("http://") || trimmed.startsWith("https://")
        val isDataUri = trimmed.startsWith("data:image/jpeg;base64,", ignoreCase = true) ||
            trimmed.startsWith("data:image/jpg;base64,", ignoreCase = true) ||
            trimmed.startsWith("data:image/png;base64,", ignoreCase = true) ||
            trimmed.startsWith("data:image/webp;base64,", ignoreCase = true)
        require(isHttp || isDataUri) {
            "Banner image must be an image picked from gallery or an http(s) URL"
        }
        return trimmed
    }

    private fun validateOptionalHttpUrl(raw: String, label: String, maxLen: Int): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "$label must start with http:// or https://"
        }
        require(trimmed.length <= maxLen) { "$label must be $maxLen characters or fewer" }
        return trimmed
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun DocumentSnapshot.toBannerNotice(): BannerNotice = BannerNotice(
        id = id,
        title = getString("title") ?: "",
        message = getString("message") ?: "",
        imageData = getString("image_data") ?: "",
        actionUrl = getString("action_url") ?: "",
        isActive = getBoolean("is_active") ?: true,
        createdAt = getTimestamp("created_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = getTimestamp("updated_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
    )

    private companion object {
        const val TAG = "BannerAnalyticsRepo"
    }
}
