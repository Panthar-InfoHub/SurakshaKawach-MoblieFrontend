package com.pantharinfohub.surakshakawach

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val firebaseUID: String,
    val displayName: String,
    val email: String,
    val gender: String,
    val emergencyContacts: List<String> = emptyList(),  // Include emergency contacts here
    val tickets: List<String> = emptyList()  // Include tickets here
)

@Serializable
data class UserProfileResponse(
    val message: String,
    val data: UserData
)
