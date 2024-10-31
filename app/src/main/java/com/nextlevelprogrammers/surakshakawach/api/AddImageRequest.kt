package com.nextlevelprogrammers.surakshakawach.api

import kotlinx.serialization.Serializable

@Serializable
data class AddImageRequest(
    val ticketId: String,
    val firebaseUID: String,
    val imageUrls: List<String>
)