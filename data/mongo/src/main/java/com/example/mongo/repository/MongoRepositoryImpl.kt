package com.example.mongo.repository

import android.provider.UserDictionary.Words.APP_ID
import com.example.util.model.Note
import com.example.util.model.RequestState
import com.example.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.ObjectId
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MongoDB : MongoRepository {
    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm


    init {
        configureTheRealm()
    }

    override fun configureTheRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(user, setOf(Note::class))
                .initialSubscriptions { sub ->
                    add(
                        query = sub.query<Note>("ownerId == $0", user.identity),
                        name = "user's Note"
                    )
                }.log(LogLevel.ALL)
                .build()
            realm = Realm.open(config)
        }
    }

    override fun getAllNotes(): Flow<Notes> {
        return if (user != null) {
            try {
                realm.query<Note>(query = "ownerId == $0", user.identity)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                        RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }

            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticationException())) }
        }
    }


    override fun getSelectedNote(noteId: ObjectId): Flow<RequestState<Note>> {
        return if (user != null) {
            try {
                realm.query<Note>(query = "_id == $0", noteId).asFlow().map {
                    RequestState.Success(data = it.list.first())
                }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticationException())) }
        }
    }


    override suspend fun addNewNote(note: Note): RequestState<Note> {
        return if (user != null) {
            realm.write {
                try {
                    val addedNote = copyToRealm(note.apply { ownerId = user.identity })
                    RequestState.Success(data = addedNote)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticationException())
        }
    }


    override suspend fun updateNote(note: Note): RequestState<Note> {
        return if (user != null) {
            realm.write {
                val queriesNote = query<Note>(query = "_id == $0", note._id).first().find()
                if (queriesNote != null) {
                    queriesNote.title = note.title
                    queriesNote.description = note.description
                    queriesNote.mood = note.mood
                    queriesNote.images = note.images
                    queriesNote.date = note.date
                    RequestState.Success(data = queriesNote)
                } else {
                    RequestState.Error(error = Exception("Queried note does not found"))
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticationException())
        }
    }

    override suspend fun deleteNote(id: ObjectId): RequestState<Note> {
        return if (user != null) {
            realm.write {
                val note =
                    query<Note>(query = "_id == $0 AND ownerId == $1", id, user.identity).first()
                        .find()
                if (note != null) {
                    try {
                        delete(note)
                        RequestState.Success(data = note)
                    } catch (e: Exception) {
                        RequestState.Error(e)
                    }
                } else {
                    RequestState.Error(Exception("Note does not exist"))
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticationException())
        }
    }


    override suspend fun deleteAllNote(): RequestState<Boolean> {
        return if (user != null) {
            realm.write {
                val notes = this.query<Note>("ownerId == $0", user.identity).find()
                try {
                    delete(notes)
                    RequestState.Success(data = true)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }
        } else {
            RequestState.Error(UserNotAuthenticationException())
        }
    }

    override fun getFilteredNotes(zonedDateTime: ZonedDateTime): Flow<Notes> {
        return if (user != null) {
            try {
                realm.query<Note>(
                    "ownerId == $0 AND date < $1 AND date > $2",
                    user.id,
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate().plusDays(1),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate(),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                ).asFlow().map { result ->
                    RequestState.Success(
                        data = result.list.groupBy {
                            it.date.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                    )
                }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticationException())) }
        }
    }

}

private class UserNotAuthenticationException : Exception("User is not Logged in.")