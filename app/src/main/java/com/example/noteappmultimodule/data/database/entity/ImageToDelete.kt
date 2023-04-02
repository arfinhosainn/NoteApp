package com.example.noteappmultimodule.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.noteappmultimodule.utils.Constants.IMAGE_TO_DELETE

@Entity(tableName = IMAGE_TO_DELETE)
data class ImageToDelete(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteImagePath: String
)
