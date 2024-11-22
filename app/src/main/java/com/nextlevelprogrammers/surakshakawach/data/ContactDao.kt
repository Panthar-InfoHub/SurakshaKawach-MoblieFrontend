package com.nextlevelprogrammers.surakshakawach.data

import androidx.room.*

@Dao
interface ContactDao {
    @Query("SELECT * FROM emergency_contacts")
    suspend fun getAllContacts(): List<EmergencyContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContactEntity>)

    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAllContacts()

    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity)

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)
}