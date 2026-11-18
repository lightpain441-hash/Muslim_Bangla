package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.MatrimonialMessageEntity
import com.example.data.local.MatrimonialProfileEntity
import com.example.data.local.MatrimonialRequestEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MatrimonialFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "ম্যাট্রিমোনিয়াল ব্যবহার করতে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।" }
        return uid
    }

    fun observeActiveProfiles(currentUid: String): Flow<List<MatrimonialProfileEntity>> = flow {
        require(currentUid.isNotBlank()) { "User ID must not be blank" }
        val path = "matrimonialProfiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatrimonialProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toUiEntity() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyProfile(currentUid: String): Flow<MatrimonialProfileEntity?> = flow {
        require(currentUid.isNotBlank()) { "User ID must not be blank" }
        val path = "matrimonialProfiles/$currentUid"
        emitAll(
            db.collection("matrimonialProfiles").document(currentUid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            MatrimonialProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.toUiEntity()
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyRequests(currentUid: String): Flow<List<MatrimonialRequestEntity>> = flow {
        require(currentUid.isNotBlank()) { "User ID must not be blank" }
        val path = "matrimonialRequests"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatrimonialRequestDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toUiEntity() }
                        .sortedByDescending { it.updatedAtMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(requestId: String, currentUid: String): Flow<List<MatrimonialMessageEntity>> = flow {
        require(currentUid.isNotBlank() && requestId.isNotBlank()) { "IDs must not be blank" }
        val path = "matrimonialRequests/$requestId/messages"
        emitAll(
            db.collection("matrimonialRequests")
                .document(requestId)
                .collection("messages")
                .whereArrayContains("participantIds", currentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatrimonialMessageDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toUiEntity(requestId) }
                        .sortedBy { it.createdAtMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertProfile(profile: MatrimonialProfileEntity): Result<Unit> {
        var docPath = "matrimonialProfiles/unauthenticated"
        return try {
            val uid = requireUserId()
            docPath = "matrimonialProfiles/$uid"
            val docRef = db.collection("matrimonialProfiles").document(uid)

            val genderSafe = if (profile.gender == "PATRI") "PATRI" else "PATRO"
            val ageSafe = profile.age.coerceIn(18L, 80L)
            val divisionSafe = profile.division.trim().take(60).ifBlank { "ঢাকা" }
            val educationSafe = profile.education.trim().take(120).ifBlank { "স্নাতক" }
            val professionSafe = profile.profession.trim().take(120).ifBlank { "পেশাজীবী" }
            val practicingSafe = profile.practicingNote.trim().take(300).ifBlank { "পাঁচ ওয়াক্ত নামাজ ও সুন্নাহ" }
            val bioSafe = profile.bioBangla.trim().take(1000).ifBlank { "ইসলামিক জীবনযাপনে আগ্রহী।" }
            val waliSafe = profile.waliRelation.trim().take(60).ifBlank { "বাবা" }
            val photoSafe = profile.photoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)

            val existingSnapshot = docRef.get().await()
            if (existingSnapshot.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "gender" to genderSafe,
                    "age" to ageSafe,
                    "division" to divisionSafe,
                    "education" to educationSafe,
                    "profession" to professionSafe,
                    "practicingNote" to practicingSafe,
                    "bioBangla" to bioSafe,
                    "waliRelation" to waliSafe,
                    "photoBlurred" to profile.photoBlurred,
                    "active" to profile.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoSafe != null) {
                    updateMap["photoUrl"] = photoSafe
                }
                docRef.update(updateMap).await()
            } else {
                val codeSafe = "MB-${uid.take(6).uppercase()}".take(20)
                val createMap = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "profileCode" to codeSafe,
                    "gender" to genderSafe,
                    "age" to ageSafe,
                    "division" to divisionSafe,
                    "education" to educationSafe,
                    "profession" to professionSafe,
                    "practicingNote" to practicingSafe,
                    "bioBangla" to bioSafe,
                    "waliRelation" to waliSafe,
                    "photoUrl" to photoSafe,
                    "photoBlurred" to profile.photoBlurred,
                    "active" to profile.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendInterestRequest(
        receiverId: String,
        senderName: String,
        receiverName: String,
        introMessage: String
    ): Result<Unit> {
        var docPath = "matrimonialRequests/unauthenticated"
        return try {
            val uid = requireUserId()
            require(receiverId.isNotBlank() && receiverId != uid) { "নিজের প্রোফাইলে প্রস্তাব পাঠানো যাবে না।" }
            val requestId = "${uid}_$receiverId"
            docPath = "matrimonialRequests/$requestId"
            val cleanSender = senderName.trim().take(100).ifBlank { "আগ্রহী সদস্য" }
            val cleanReceiver = receiverName.trim().take(100).ifBlank { "সদস্য" }
            val cleanIntro = introMessage.trim().take(400)
                .ifBlank { "আসসালামু আলাইকুম। অভিভাবকের মাধ্যমে কথা বলতে আগ্রহী।" }

            val payload = mapOf(
                "senderId" to uid,
                "receiverId" to receiverId,
                "participantIds" to listOf(uid, receiverId),
                "senderName" to cleanSender,
                "receiverName" to cleanReceiver,
                "status" to "PENDING",
                "introMessage" to cleanIntro,
                "photoRevealApproved" to false,
                "waliVerified" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("matrimonialRequests").document(requestId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun respondToRequest(
        requestId: String,
        accept: Boolean,
        approvePhotoReveal: Boolean
    ): Result<Unit> {
        val docPath = "matrimonialRequests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID must not be blank" }
            db.collection("matrimonialRequests").document(requestId).update(
                mapOf(
                    "status" to if (accept) "ACCEPTED" else "DECLINED",
                    "photoRevealApproved" to (accept && approvePhotoReveal),
                    "waliVerified" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun togglePhotoReveal(requestId: String, approved: Boolean): Result<Unit> {
        val docPath = "matrimonialRequests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank()) { "Request ID must not be blank" }
            db.collection("matrimonialRequests").document(requestId).update(
                mapOf(
                    "photoRevealApproved" to approved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(requestId: String, text: String): Result<Unit> {
        var docPath = "matrimonialRequests/$requestId/messages"
        return try {
            val uid = requireUserId()
            require(requestId.isNotBlank()) { "Request ID must not be blank" }
            val clean = text.trim().take(1000)
            require(clean.isNotEmpty()) { "বার্তা খালি রাখা যাবে না।" }

            val reqSnap = db.collection("matrimonialRequests").document(requestId).get().await()
            val req = reqSnap.toObject(MatrimonialRequestDoc::class.java)
                ?: throw IllegalStateException("প্রস্তাব খুঁজে পাওয়া যায়নি।")
            require(req.status == "ACCEPTED") { "অনুমোদিত প্রস্তাব ছাড়া বার্তা পাঠানো যাবে না।" }
            require(uid in req.participantIds && req.participantIds.size == 2) {
                "এই কথোপকথনে অংশ নেওয়ার অনুমতি নেই।"
            }

            val messageId = UUID.randomUUID().toString()
            docPath = "matrimonialRequests/$requestId/messages/$messageId"
            val payload = mapOf(
                "senderId" to uid,
                "participantIds" to req.participantIds,
                "text" to clean,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("matrimonialRequests")
                .document(requestId)
                .collection("messages")
                .document(messageId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
