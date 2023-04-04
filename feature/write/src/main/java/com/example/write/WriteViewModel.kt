package com.example.write

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mongo.database.ImageToDeleteDao
import com.example.mongo.database.ImageToUploadDao
import com.example.mongo.database.entity.ImageToDelete
import com.example.mongo.database.entity.ImageToUpload
import com.example.mongo.repository.MongoDB
import com.example.ui.GalleryImage
import com.example.ui.GalleryState
import com.example.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.example.util.fetchImagesFromFirebase
import com.example.util.model.Mood
import com.example.util.model.Note
import com.example.util.model.RequestState
import com.example.util.toRealmInstant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.ObjectId
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject


@HiltViewModel
internal class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imagesToUploadDao: ImageToUploadDao,
    private val imagesToDeleteDao: ImageToDeleteDao
) : ViewModel() {


    val galleryState = GalleryState()

    var uiState by mutableStateOf(UiState())
        private set

    init {
        getNoteArgument()
        fetchSelectedNote()
    }

    private fun getNoteArgument() {
        uiState = uiState.copy(
            selectedNoteId = savedStateHandle.get<String>(
                key = WRITE_SCREEN_ARGUMENT_KEY
            )
        )
    }


    fun upsertNote(
        note: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedNoteId != null) {
                updateNote(note, onSuccess, onError)

            } else {
                insertNote(note, onSuccess, onError)
            }
        }
    }


    private suspend fun updateNote(
        note: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = MongoDB.updateNote(note.apply {
            _id = ObjectId.Companion.from(uiState.selectedNoteId!!)
            date = if (uiState.updateDateTime != null) {
                uiState.updateDateTime!!
            } else {
                uiState.selectedNote!!.date
            }
        })

        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            deleteImagesFromFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    private fun deleteImagesFromFirebase(images: List<String>? = null) {
        val storage = FirebaseStorage.getInstance().reference
        if (images != null) {
            images.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )

                        }
                    }
            }
        } else {
            galleryState.imagesToBeDeleted.map {
                it.remoteImagePath
            }.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToDeleteDao.addImageToDelete(
                                ImageToDelete(
                                    remoteImagePath = remotePath
                                )
                            )

                        }
                    }
            }
        }
    }


    private fun fetchSelectedNote() {
        if (uiState.selectedNoteId != null) {
            viewModelScope.launch {
                MongoDB.getSelectedNote(
                    noteId = ObjectId.Companion.from(uiState.selectedNoteId!!)
                ).catch { emit(RequestState.Error(Exception("Note is already deleted"))) }
                    .collect { note ->
                        if (note is RequestState.Success) {
                            setSelectedNote(note = note.data)
                            setTitle(note.data.title)
                            setDescription(note.data.description)
                            setMood(mood = Mood.valueOf(note.data.mood))

                            fetchImagesFromFirebase(
                                remoteImagePaths = note.data.images,
                                onImageDownload = { downloadedImage ->
                                    galleryState.addImage(
                                        GalleryImage(
                                            image = downloadedImage,
                                            remoteImagePath = extractImagePath(
                                                fullImageUrl = downloadedImage.toString()
                                            )
                                        )
                                    )

                                }
                            )
                        }
                    }
            }
        }
    }

    private fun extractImagePath(fullImageUrl: String): String {
        val chunks = fullImageUrl.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"

    }

    fun addImage(image: Uri, imageType: String) {
        val remoteImagePath = "images/${FirebaseAuth.getInstance().currentUser?.uid}/" +
                "${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"

        Log.d("writeviewmodel", "addImage: $remoteImagePath")

        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun uploadImagesToFirebase() {
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { galleryImage ->
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if (sessionUri != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }

                    }
                }
        }
    }


    fun deleteNote(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedNoteId != null) {
                if (uiState.selectedNoteId != null) {
                    val result =
                        MongoDB.deleteNote(id = ObjectId.from(uiState.selectedNoteId!!))
                    if (result is RequestState.Success) {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                            uiState.selectedNote?.let { deleteImagesFromFirebase(images = it.images) }
                        }
                    } else if (result is RequestState.Error) {
                        withContext(Dispatchers.Main) {
                            onError(result.error.message.toString())
                        }
                    }
                }
            }
        }
    }


    private fun insertNote(
        note: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MongoDB.addNewNote(note.apply {
                if (uiState.updateDateTime != null) {
                    date = uiState.updateDateTime!!
                }
            })
            if (result is RequestState.Success) {
                uploadImagesToFirebase()
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else if (result is RequestState.Error) {
                withContext(Dispatchers.Main) {
                    onError(result.error.message.toString())
                }
            }
        }
    }


    fun setTitle(title: String) {
        uiState = uiState.copy(title = title)
    }

    fun updateDateTime(zonedDateTime: ZonedDateTime) {
        uiState =
            uiState.copy(updateDateTime = zonedDateTime.toInstant().toRealmInstant())

    }


    fun setDescription(description: String) {
        uiState = uiState.copy(description = description)
    }

    private fun setMood(mood: Mood) {
        uiState = uiState.copy(mood = mood)
    }

    private fun setSelectedNote(note: Note) {
        uiState = uiState.copy(selectedNote = note)
    }


}


internal data class UiState(
    val selectedNoteId: String? = null,
    val title: String = "",
    val selectedNote: Note? = null,
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updateDateTime: RealmInstant? = null
)