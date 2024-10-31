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
