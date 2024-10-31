package com.nextlevelprogrammers.surakshakawach.api

import kotlinx.serialization.Serializable

@Serializable
data
class AddAudioRequest(
    val ticketId: String,
    val firebaseUID: String,
    val clipUrls: List<String>
)
