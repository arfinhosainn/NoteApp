package com.example.noteappmultimodule.data

import com.example.util.model.Note
import com.example.util.model.RequestState
import io.realm.kotlin.types.ObjectId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZonedDateTime


typealias Notes = RequestState<Map<LocalDate, List<Note>>>

interface MongoRepository {

    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>
    fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>>

    suspend fun addNewNote(note: Note): RequestState<Note>

    suspend fun updateNote(note: Note): RequestState<Note>

    suspend fun deleteNote(id: ObjectId): RequestState<Note>
    suspend fun deleteAllNote(): RequestState<Boolean>

    fun getFilteredNotes(zonedDateTime: ZonedDateTime):Flow<Notes>

}