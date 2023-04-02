package com.example.noteappmultimodule.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.noteappmultimodule.data.database.entity.ImageToDelete

@Dao
interface ImageToDeleteDao {

    @Query("SELECT * FROM image_to_delete ORDER BY id ASC")
    suspend fun getAllImages():List<ImageToDelete>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addImageToDelete(imageToDelete: ImageToDelete)

    @Query("DELETE FROM image_to_delete WHERE id=:imageId")
    suspend fun cleanUpImage(imageId: Int)


}