package com.nextlevelprogrammers.surakshakawach.api

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val firebaseUID: String,
    val displayName: String,
    val email: String,
    val gender: String,
    val emergencyContacts: List<EmergencyContact>? = null,  // Include emergency contacts here
    val tickets: List<String>? = null  // Include tickets here
)

@Serializable
data class UserProfileResponse(
    val message: String,
    val data: UserData
)

@Serializable
data class EmergencyContact(
    val name: String,
    val email: String,
    val mobile: String
)

@Serializable
data class UpdateEmergencyContactRequest(
    val firebaseUID: String,
    val old_contacts: List<EmergencyContact>,
    val new_contacts: List<EmergencyContact>
)

@Serializable
data class RemoveEmergencyContactRequest(
    val firebaseUID: String,
    val contact_details: List<EmergencyContact>
)