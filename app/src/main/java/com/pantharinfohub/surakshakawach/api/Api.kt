package com.pantharinfohub.surakshakawach.api

import com.pantharinfohub.surakshakawach.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.*
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class Api {

    // Initialize Ktor client with content negotiation plugin
    private val client = HttpClient {
        install(ContentNegotiation) {
            json() // JSON will still be used if needed elsewhere
        }
    }

    // Function to send user data to the server using x-www-form-urlencoded
    suspend fun createUser(
        firebaseUID: String,
        name: String,
        email: String,
        gender: String
    ): Boolean {
        val response: HttpResponse = client.submitForm(
            url = "https://surakshakawach-mobilebackend-192854867616.asia-south2.run.app/api/v1/user",
            formParameters = Parameters.build {
                append("firebaseUID", firebaseUID)
                append("name", name)
                append("email", email)
                append("gender", gender)
            }
        )

        return response.status == HttpStatusCode.OK // Assuming the API returns 201 Created on success
    }

    //Function to check user exists in firebase through UID
    suspend fun checkIfUserExists(firebaseUID: String): Boolean {
        val response: HttpResponse = client.submitForm(
            url = "https://surakshakawach-mobilebackend-192854867616.asia-south2.run.app/api/v1/user/check",
            formParameters = Parameters.build {
                append("firebaseUID", firebaseUID)
            }
        )
        return response.status == HttpStatusCode.OK // Assuming the API returns 200 if user exists
    }

    // Configure JSON parser with `ignoreUnknownKeys`
    private val json = Json {
        ignoreUnknownKeys = true  // This will ignore fields like `emergencyContacts` and `tickets`
    }

    // Function to get user profile data from the server using GET request
    suspend fun getUserProfile(firebaseUID: String): UserProfileResponse? {
        val response: HttpResponse = client.get("https://surakshakawach-mobilebackend-192854867616.asia-south2.run.app/api/v1/user") {
            url {
                parameters.append("firebaseUID", firebaseUID)
            }
        }

        return if (response.status == HttpStatusCode.OK) {
            // Deserialize response body to UserProfileResponse, ignoring unknown fields
            json.decodeFromString(response.body())
        } else {
            null
        }
    }

    // Function to send emergency contact data to the server using x-www-form-urlencoded
    suspend fun sendEmergencyContactToServer(
        firebaseUID: String,
        name: String,
        email: String,
        mobile: String
    ): Boolean {
        val response: HttpResponse = client.submitForm(
            url = "https://surakshakawach-mobilebackend-192854867616.asia-south2.run.app/api/v1/user/create/emergency-contact",
            formParameters = Parameters.build {
                append("firebaseUID", firebaseUID)
                append("name", name)
                append("email", email)
                append("mobile", mobile)
            }
        )

        return response.status == HttpStatusCode.Created
    }

    // Function to send SOS ticket to the server using x-www-form-urlencoded
    suspend fun sendSosTicket(
        firebaseUID: String,
        latitude: String,
        longitude: String,
        timestamp: String
    ): Boolean {
        val response: HttpResponse = client.submitForm(
            url = "https://surakshakawach-mobilebackend-192854867616.asia-south2.run.app/api/v1/ticket",
            formParameters = Parameters.build {
                append("firebaseUID", firebaseUID)
                append("latitude", latitude)
                append("longitude", longitude)
                append("timestamp", timestamp)
            }
        )

        return response.status == HttpStatusCode.Created // Assuming the API returns 201 Created on success
    }

}