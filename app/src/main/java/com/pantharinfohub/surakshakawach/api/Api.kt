package com.pantharinfohub.surakshakawach.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json

class Api {

    // Initialize Ktor client with content negotiation plugin
    private val client = HttpClient {
        install(ContentNegotiation) {
            json() // JSON will still be used if needed elsewhere
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
}