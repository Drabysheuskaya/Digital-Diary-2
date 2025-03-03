package com.darina.PRM_2_S25580.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "entities")
data class Entity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val photoData: ByteArray?,
    val voiceRecordingData: String?,
    val latitude: Double,
    val longitude: Double,
    val location: String? = null,
    val timestamp: Long = Date().time
)
