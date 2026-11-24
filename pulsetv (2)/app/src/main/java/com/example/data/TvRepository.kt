package com.example.data

import android.content.Context
import com.example.config.FirebaseConfig
import com.example.model.BrokenReport
import com.example.model.Channel
import com.example.model.ChannelCatalogDefaults
import com.example.model.LiveEvent
import com.example.model.LiveEventDefaults
import com.example.model.UpdateNotice
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

class TvRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(FirebaseConfig.getFirestore(context))

    private fun requireAdminSession(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in with an authorized Google account to perform admin actions.")
    }

    fun observeChannels(): Flow<List<Channel>> =
        db.collection(FirebaseConfig.COLLECTION_CHANNELS)
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toChannel() }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(
                    firestoreError ?: error,
                    OperationType.LIST,
                    FirebaseConfig.COLLECTION_CHANNELS,
                )
                throw error
            }

    fun observeLiveEvents(): Flow<List<LiveEvent>> =
        db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toLiveEvent() }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(
                    firestoreError ?: error,
                    OperationType.LIST,
                    FirebaseConfig.COLLECTION_LIVE_EVENTS,
                )
                throw error
            }

    fun observeUpdatesInbox(): Flow<List<UpdateNotice>> =
        db.collection(FirebaseConfig.COLLECTION_UPDATES_INBOX)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toUpdateNotice() }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(
                    firestoreError ?: error,
                    OperationType.LIST,
                    FirebaseConfig.COLLECTION_UPDATES_INBOX,
                )
                throw error
            }

    fun observeBrokenReports(): Flow<List<BrokenReport>> =
        db.collection(FirebaseConfig.COLLECTION_BROKEN_REPORTS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toBrokenReport() }
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(
                        firestoreError ?: error,
                        OperationType.LIST,
                        FirebaseConfig.COLLECTION_BROKEN_REPORTS,
                    )
                }
                throw error
            }

    suspend fun submitBrokenReport(channel: Channel, reason: String): Result<Unit> {
        val docRef = db.collection(FirebaseConfig.COLLECTION_BROKEN_REPORTS).document()
        return try {
            val cleanId = channel.id.trim()
            val cleanName = channel.name.trim().take(100)
            val cleanReason = reason.trim().take(500)
            require(cleanId.isNotBlank()) { "Channel ID cannot be blank" }
            require(cleanName.isNotBlank()) { "Channel name cannot be blank" }
            require(cleanReason.isNotBlank()) { "Reason cannot be blank" }

            val payload = mapOf(
                "channel_id" to cleanId,
                "channel_name" to cleanName,
                "reason" to cleanReason,
                "timestamp" to FieldValue.serverTimestamp(),
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

    suspend fun createChannel(
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val docRef = db.collection(FirebaseConfig.COLLECTION_CHANNELS).document()
        return try {
            requireAdminSession()
            val payload = buildChannelCreateMap(
                name = name,
                category = category,
                language = language,
                country = country,
                logo = logo,
                primaryUrl = primaryUrl,
                backupUrl = backupUrl,
                isActive = isActive,
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

    suspend fun updateChannel(
        channelId: String,
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_CHANNELS}/$channelId"
        return try {
            requireAdminSession()
            require(channelId.isNotBlank()) { "Channel ID cannot be blank" }
            val cleanName = validateRequiredText(name, "Channel name", 80)
            val cleanCategory = validateRequiredText(category, "Category", 40)
            val cleanLanguage = validateRequiredText(language, "Language", 40)
            val cleanCountry = validateRequiredText(country.ifBlank { "Global" }, "Country", 50)
            val cleanPrimary = validateStreamUrl(primaryUrl, required = true)
            val cleanBackup = validateStreamUrl(backupUrl, required = false)
            val cleanLogo = logo.trim().take(500)

            val updates = mapOf(
                "name" to cleanName,
                "category" to cleanCategory,
                "language" to cleanLanguage,
                "country" to cleanCountry,
                "logo" to cleanLogo,
                "primary_url" to cleanPrimary,
                "backup_url" to cleanBackup,
                "is_active" to isActive,
                "updated_at" to FieldValue.serverTimestamp(),
            )
            db.collection(FirebaseConfig.COLLECTION_CHANNELS).document(channelId)
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

    suspend fun toggleChannelActive(channelId: String, isActive: Boolean): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_CHANNELS}/$channelId"
        return try {
            requireAdminSession()
            require(channelId.isNotBlank()) { "Channel ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_CHANNELS).document(channelId)
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

    suspend fun deleteChannel(channelId: String): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_CHANNELS}/$channelId"
        return try {
            requireAdminSession()
            require(channelId.isNotBlank()) { "Channel ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_CHANNELS).document(channelId)
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

    /**
     * Imports a parsed M3U / playlist batch into Cloud Firestore in safe chunks of 400
     * operations, skipping any stream URLs or deterministic IDs already present.
     */
    suspend fun importChannelsBatch(channels: List<Channel>): Result<Int> {
        return try {
            requireAdminSession()
            if (channels.isEmpty()) return Result.success(0)

            val existingSnap = db.collection(FirebaseConfig.COLLECTION_CHANNELS).get().await()
            val existingIds = existingSnap.documents.mapTo(HashSet()) { it.id }
            val existingUrls = existingSnap.documents.mapNotNullTo(HashSet()) {
                it.getString("primary_url")?.trim()
            }

            val newEntries = ArrayList<Pair<String, Map<String, Any>>>(channels.size)
            val batchSeenUrls = HashSet<String>()

            for (ch in channels) {
                val url = ch.primaryUrl.trim()
                if (url.isBlank() || url in existingUrls || !batchSeenUrls.add(url)) continue
                val candidateId = ch.id.trim()
                    .takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$")) }
                    ?: com.example.model.M3uPlaylistParser.stableDocId(url)
                if (candidateId in existingIds) continue

                val payload = runCatching {
                    buildChannelCreateMap(
                        name = ch.name,
                        category = ch.category,
                        language = ch.language,
                        country = ch.country,
                        logo = ch.logo,
                        primaryUrl = url,
                        backupUrl = ch.backupUrl,
                        isActive = ch.isActive,
                    )
                }.getOrNull() ?: continue

                existingIds.add(candidateId)
                newEntries.add(candidateId to payload)
            }

            if (newEntries.isEmpty()) return Result.success(0)

            var importedCount = 0
            for (chunk in newEntries.chunked(400)) {
                val batch = db.batch()
                for ((docId, payload) in chunk) {
                    val docRef = db.collection(FirebaseConfig.COLLECTION_CHANNELS).document(docId)
                    batch.set(docRef, payload)
                }
                batch.commit().await()
                importedCount += chunk.size
            }
            Result.success(importedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, FirebaseConfig.COLLECTION_CHANNELS)
            Result.failure(e)
        }
    }

    suspend fun publishUpdateNotice(
        title: String,
        message: String,
        category: String = "Notice",
    ): Result<Unit> {
        val docRef = db.collection(FirebaseConfig.COLLECTION_UPDATES_INBOX).document()
        return try {
            requireAdminSession()
            val cleanTitle = validateRequiredText(title, "Notice title", 120)
            val cleanMessage = validateRequiredText(message, "Notice message", 1000)
            val cleanCategory = category.trim().ifBlank { "Notice" }.take(40)

            val payload = mapOf(
                "title" to cleanTitle,
                "message" to cleanMessage,
                "category" to cleanCategory,
                "timestamp" to FieldValue.serverTimestamp(),
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

    suspend fun deleteUpdateNotice(updateId: String): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_UPDATES_INBOX}/$updateId"
        return try {
            requireAdminSession()
            require(updateId.isNotBlank()) { "Update ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_UPDATES_INBOX).document(updateId)
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

    suspend fun deleteBrokenReport(reportId: String): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_BROKEN_REPORTS}/$reportId"
        return try {
            requireAdminSession()
            require(reportId.isNotBlank()) { "Report ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_BROKEN_REPORTS).document(reportId)
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

    suspend fun clearAllBrokenReports(reportIds: List<String>): Result<Unit> {
        return try {
            requireAdminSession()
            if (reportIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            reportIds.take(400).forEach { id ->
                if (id.isNotBlank()) {
                    batch.delete(db.collection(FirebaseConfig.COLLECTION_BROKEN_REPORTS).document(id))
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, FirebaseConfig.COLLECTION_BROKEN_REPORTS)
            Result.failure(e)
        }
    }

    suspend fun seedPublicTestChannels(): Result<Unit> {
        return try {
            requireAdminSession()
            val batch = db.batch()
            ChannelCatalogDefaults.PUBLIC_HLS_STARTERS.forEach { ch ->
                val docRef = db.collection(FirebaseConfig.COLLECTION_CHANNELS).document()
                val payload = buildChannelCreateMap(
                    name = ch.name,
                    category = ch.category,
                    language = ch.language,
                    country = ch.country,
                    logo = ch.logo,
                    primaryUrl = ch.primaryUrl,
                    backupUrl = ch.backupUrl,
                    isActive = ch.isActive,
                )
                batch.set(docRef, payload)
            }
            val noticeRef = db.collection(FirebaseConfig.COLLECTION_UPDATES_INBOX).document()
            batch.set(
                noticeRef,
                mapOf(
                    "title" to "Live Channels Synced",
                    "message" to "Starter HLS streams are live. Changes made in the Admin Panel update across every connected device in real time.",
                    "category" to "Channel Update",
                    "timestamp" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, FirebaseConfig.COLLECTION_CHANNELS)
            Result.failure(e)
        }
    }

    suspend fun createLiveEvent(
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val docRef = db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS).document()
        return try {
            requireAdminSession()
            val payload = buildLiveEventCreateMap(
                title = title,
                sport = sport,
                teams = teams,
                status = status,
                thumbnail = thumbnail,
                streamUrl = streamUrl,
                backupUrl = backupUrl,
                isActive = isActive,
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

    suspend fun updateLiveEvent(
        eventId: String,
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_LIVE_EVENTS}/$eventId"
        return try {
            requireAdminSession()
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            val cleanTitle = validateRequiredText(title, "Event title", 100)
            val cleanSport = validateRequiredText(sport, "Sport / Category", 40)
            val cleanTeams = teams.trim().take(100)
            val cleanStatus = status.trim().uppercase().takeIf { it in LiveEventDefaults.STATUSES } ?: "LIVE"
            val cleanThumbnail = validateStreamUrl(thumbnail, required = false, maxLen = 500, label = "Thumbnail URL")
            val cleanStream = validateStreamUrl(streamUrl, required = true)
            val cleanBackup = validateStreamUrl(backupUrl, required = false)

            val updates = mapOf(
                "title" to cleanTitle,
                "sport" to cleanSport,
                "teams" to cleanTeams,
                "status" to cleanStatus,
                "thumbnail" to cleanThumbnail,
                "stream_url" to cleanStream,
                "backup_url" to cleanBackup,
                "is_active" to isActive,
                "updated_at" to FieldValue.serverTimestamp(),
            )
            db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS).document(eventId)
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

    suspend fun toggleLiveEventActive(eventId: String, isActive: Boolean): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_LIVE_EVENTS}/$eventId"
        return try {
            requireAdminSession()
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS).document(eventId)
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

    suspend fun deleteLiveEvent(eventId: String): Result<Unit> {
        val path = "${FirebaseConfig.COLLECTION_LIVE_EVENTS}/$eventId"
        return try {
            requireAdminSession()
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS).document(eventId)
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

    suspend fun seedStarterLiveEvents(): Result<Unit> {
        return try {
            requireAdminSession()
            val batch = db.batch()
            LiveEventDefaults.STARTER_EVENTS.forEach { evt ->
                val docRef = db.collection(FirebaseConfig.COLLECTION_LIVE_EVENTS).document()
                val payload = buildLiveEventCreateMap(
                    title = evt.title,
                    sport = evt.sport,
                    teams = evt.teams,
                    status = evt.status,
                    thumbnail = evt.thumbnail,
                    streamUrl = evt.streamUrl,
                    backupUrl = evt.backupUrl,
                    isActive = evt.isActive,
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, FirebaseConfig.COLLECTION_LIVE_EVENTS)
            Result.failure(e)
        }
    }

    private fun buildLiveEventCreateMap(
        title: String,
        sport: String,
        teams: String,
        status: String,
        thumbnail: String,
        streamUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Map<String, Any> {
        val cleanTitle = validateRequiredText(title, "Event title", 100)
        val cleanSport = validateRequiredText(sport, "Sport / Category", 40)
        val cleanTeams = teams.trim().take(100)
        val cleanStatus = status.trim().uppercase().takeIf { it in LiveEventDefaults.STATUSES } ?: "LIVE"
        val cleanThumbnail = validateStreamUrl(thumbnail, required = false, maxLen = 500, label = "Thumbnail URL")
        val cleanStream = validateStreamUrl(streamUrl, required = true)
        val cleanBackup = validateStreamUrl(backupUrl, required = false)

        return mapOf(
            "title" to cleanTitle,
            "sport" to cleanSport,
            "teams" to cleanTeams,
            "status" to cleanStatus,
            "thumbnail" to cleanThumbnail,
            "stream_url" to cleanStream,
            "backup_url" to cleanBackup,
            "is_active" to isActive,
            "created_at" to FieldValue.serverTimestamp(),
            "updated_at" to FieldValue.serverTimestamp(),
        )
    }

    private fun buildChannelCreateMap(
        name: String,
        category: String,
        language: String,
        country: String,
        logo: String,
        primaryUrl: String,
        backupUrl: String,
        isActive: Boolean,
    ): Map<String, Any> {
        val cleanName = validateRequiredText(name, "Channel name", 80)
        val cleanCategory = validateRequiredText(category, "Category", 40)
        val cleanLanguage = validateRequiredText(language, "Language", 40)
        val cleanCountry = validateRequiredText(country.ifBlank { "Global" }, "Country", 50)
        val cleanPrimary = validateStreamUrl(primaryUrl, required = true)
        val cleanBackup = validateStreamUrl(backupUrl, required = false)
        val cleanLogo = logo.trim().take(500)

        return mapOf(
            "name" to cleanName,
            "category" to cleanCategory,
            "language" to cleanLanguage,
            "country" to cleanCountry,
            "logo" to cleanLogo,
            "primary_url" to cleanPrimary,
            "backup_url" to cleanBackup,
            "is_active" to isActive,
            "created_at" to FieldValue.serverTimestamp(),
            "updated_at" to FieldValue.serverTimestamp(),
        )
    }

    private fun validateRequiredText(raw: String, label: String, maxLen: Int): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "$label is required" }
        require(trimmed.length <= maxLen) { "$label must be $maxLen characters or fewer" }
        return trimmed
    }

    private fun validateStreamUrl(
        raw: String,
        required: Boolean,
        maxLen: Int = 1000,
        label: String = "Stream URL",
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            require(!required) { "Primary stream URL is required" }
            return ""
        }
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

    private fun DocumentSnapshot.toChannel(): Channel = Channel(
        id = id,
        name = getString("name") ?: "",
        category = getString("category") ?: "Entertainment",
        language = getString("language") ?: "English",
        country = getString("country")?.ifBlank { "Global" } ?: "Global",
        logo = getString("logo") ?: "",
        primaryUrl = getString("primary_url") ?: "",
        backupUrl = getString("backup_url") ?: "",
        isActive = getBoolean("is_active") ?: true,
        createdAt = getTimestamp("created_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = getTimestamp("updated_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
    )

    private fun DocumentSnapshot.toLiveEvent(): LiveEvent = LiveEvent(
        id = id,
        title = getString("title") ?: "",
        sport = getString("sport") ?: "Cricket",
        teams = getString("teams") ?: "",
        status = getString("status") ?: "LIVE",
        thumbnail = getString("thumbnail") ?: "",
        streamUrl = getString("stream_url") ?: "",
        backupUrl = getString("backup_url") ?: "",
        isActive = getBoolean("is_active") ?: true,
        createdAt = getTimestamp("created_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = getTimestamp("updated_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
    )

    private fun DocumentSnapshot.toUpdateNotice(): UpdateNotice = UpdateNotice(
        id = id,
        title = getString("title") ?: "",
        message = getString("message") ?: "",
        category = getString("category") ?: "Notice",
        timestamp = getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
    )

    private fun DocumentSnapshot.toBrokenReport(): BrokenReport = BrokenReport(
        id = id,
        channelId = getString("channel_id") ?: "",
        channelName = getString("channel_name") ?: "",
        reason = getString("reason") ?: "",
        timestamp = getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
    )
}
