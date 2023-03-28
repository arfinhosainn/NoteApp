package com.example.noteappmultimodule.data

import com.example.noteappmultimodule.model.Note
import com.example.noteappmultimodule.model.RequestState
import com.example.noteappmultimodule.utils.Constants.APP_ID
import com.example.noteappmultimodule.utils.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

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


}

private class UserNotAuthenticationException : Exception("User is not Logged in.")