package com.nextlevelprogrammers.surakshakawach.api

import kotlinx.serialization.Serializable

@Serializable
data class ImageData(
    val url: String,
    val timestamp: Long,
    val gsBucketUrl: String
)

@Serializable
data class AddImageRequest(
    val ticketId: String,
    val firebaseUID: String,
    val imagesData: List<ImageData>
)