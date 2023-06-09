package com.example.noteappmultimodule.di

import android.content.Context
import androidx.room.Room
import com.example.mongo.database.ImagesDatabase
import com.example.util.Constants.IMAGES_DATABASE
import com.example.util.connectivity.NetworkConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesImagesDatabase(@ApplicationContext context: Context): ImagesDatabase {
        return Room.databaseBuilder(
            context,
            klass = ImagesDatabase::class.java,
            name = IMAGES_DATABASE
        ).build()
    }


    @Singleton
    @Provides
    fun providesFirstDao(database: ImagesDatabase) = database.imageToUpload()

    @Singleton
    @Provides
    fun providesSecondDao(database: ImagesDatabase) = database.imageToDelete()


    @Singleton
    @Provides
    fun provideNetworkConnectivityObserver(@ApplicationContext context: Context) =
        NetworkConnectivityObserver(context = context)

}