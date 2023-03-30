package com.example.noteappmultimodule.data

import com.example.noteappmultimodule.model.Note
import com.example.noteappmultimodule.model.RequestState
import io.realm.kotlin.types.ObjectId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate


typealias Notes = RequestState<Map<LocalDate, List<Note>>>

interface MongoRepository {

    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>
    fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>>

    suspend fun addNewNote(note: Note): RequestState<Note>

    suspend fun updateNote(note: Note): RequestState<Note>

    suspend fun deleteNote(id: ObjectId):RequestState<Note>

}