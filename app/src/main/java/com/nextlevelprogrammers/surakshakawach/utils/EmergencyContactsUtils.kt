package com.nextlevelprogrammers.surakshakawach.utils

import com.nextlevelprogrammers.surakshakawach.api.EmergencyContact
import com.nextlevelprogrammers.surakshakawach.data.EmergencyContactEntity

// Convert API model to Room entity
fun EmergencyContact.toEntity() = EmergencyContactEntity(
    name = this.name,
    email = this.email,
    mobile = this.mobile
)

// Convert Room entity to API model
fun EmergencyContactEntity.toApiModel() = EmergencyContact(
    name = this.name,
    email = this.email,
    mobile = this.mobile
)