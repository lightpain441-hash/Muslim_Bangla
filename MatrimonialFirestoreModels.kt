package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.local.MatrimonialMessageEntity
import com.example.data.local.MatrimonialProfileEntity
import com.example.data.local.MatrimonialRequestEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class MatrimonialProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileCode: String = "",
    val gender: String = "PATRO",
    val age: Long = 25L,
    val division: String = "ঢাকা",
    val education: String = "",
    val profession: String = "",
    val practicingNote: String = "",
    val bioBangla: String = "",
    val waliRelation: String = "বাবা",
    val photoUrl: String? = null,
    val photoBlurred: Boolean = true,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toUiEntity(): MatrimonialProfileEntity = MatrimonialProfileEntity(
        id = id.ifBlank { userId },
        userId = userId,
        profileCode = profileCode,
        gender = if (gender == "PATRI") "PATRI" else "PATRO",
        age = age.coerceIn(18L, 80L),
        division = division,
        education = education,
        profession = profession,
        practicingNote = practicingNote,
        bioBangla = bioBangla,
        waliRelation = waliRelation,
        photoUrl = photoUrl,
        photoBlurred = photoBlurred,
        active = active,
        updatedAtMillis = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class MatrimonialRequestDoc(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderName: String = "",
    val receiverName: String = "",
    val status: String = "PENDING",
    val introMessage: String = "",
    val photoRevealApproved: Boolean = false,
    val waliVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toUiEntity(): MatrimonialRequestEntity = MatrimonialRequestEntity(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        senderName = senderName,
        receiverName = receiverName,
        status = when (status) {
            "ACCEPTED", "DECLINED", "PENDING" -> status
            else -> "PENDING"
        },
        introMessage = introMessage,
        photoRevealApproved = photoRevealApproved,
        waliVerified = waliVerified,
        createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAtMillis = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class MatrimonialMessageDoc(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toUiEntity(requestId: String): MatrimonialMessageEntity = MatrimonialMessageEntity(
        id = id,
        requestId = requestId,
        senderId = senderId,
        text = text,
        createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}
