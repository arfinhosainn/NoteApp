package com.example.noteappmultimodule.data

import com.example.noteappmultimodule.model.Note
import com.example.noteappmultimodule.model.RequestState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate


typealias Notes = RequestState<Map<LocalDate, List<Note>>>

interface MongoRepository {

    fun configureTheRealm()
    fun getAllNotes(): Flow<Notes>

}