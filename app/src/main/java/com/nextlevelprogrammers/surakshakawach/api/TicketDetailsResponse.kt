package com.nextlevelprogrammers.surakshakawach.api

import kotlinx.serialization.Serializable

@Serializable
data class TicketDetailsResponse(
    val ticketId: String,
    val firebaseUID: String,
    val locationInfo: List<LocationInfo>,
    val status: String
)

@Serializable
data class LocationInfo(
    val coordinates: Coordinates,
    val timestamp: String
)

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class ApiResponse(
    val message: String,
    val data: DataContent
)

@Serializable
data class DataContent(
    val user: UserInfo? = null,    // Make user nullable
    val ticket: TicketDetailsResponse? = null,  // Make ticket nullable
    val locationInfo: LocationInfo? = null  // Include locationInfo as nullable for specific response
)

@Serializable
data class UserInfo(
    val firebaseUID: String,
    val displayName: String,
    val email: String,
    val gender: String,
    val emergencyContacts: List<EmergencyContact>,
    val tickets: List<String>
)

data class TicketInfo(
    val status: String?,
    val userName: String?
)