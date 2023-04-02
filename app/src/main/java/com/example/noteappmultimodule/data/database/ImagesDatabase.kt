package com.example.noteappmultimodule.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.noteappmultimodule.data.database.entity.ImageToUpload

@Database(entities = [ImageToUpload::class], version = 1, exportSchema = false)
abstract class ImagesDatabase : RoomDatabase() {

    abstract fun imageToUpload(): ImageToUploadDao

}