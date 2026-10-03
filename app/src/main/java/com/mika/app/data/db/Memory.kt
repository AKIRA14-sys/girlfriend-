package com.mika.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String,
    val timestamp: Long = System.currentTimeMillis()
)
