package com.example.noteappmultimodule.presentation.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.noteappmultimodule.connectivity.ConnectivityObserver
import com.example.noteappmultimodule.connectivity.NetworkConnectivityObserver
import com.example.noteappmultimodule.data.MongoDB
import com.example.noteappmultimodule.data.Notes
import com.example.noteappmultimodule.data.database.ImageToDeleteDao
import com.example.noteappmultimodule.data.database.entity.ImageToDelete
import com.example.noteappmultimodule.model.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
) : ViewModel() {
    var notes: MutableState<Notes> = mutableStateOf(RequestState.Idle)

    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)


    init {
        observeAllNotes()
        viewModelScope.launch {
            connectivity.observe().collect {
                network = it
            }
        }
    }

    fun deleteAllNotes(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {

        if (network == ConnectivityObserver.Status.Available) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference
            storage.child(imagesDirectory)
                .listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        val imagePath = "images/${userId}/${ref.name}"
                        storage.child(imagePath).delete()
                            .addOnFailureListener {
                                viewModelScope.launch(Dispatchers.IO) {
                                    imageToDeleteDao.addImageToDelete(
                                        ImageToDelete(
                                            remoteImagePath = imagePath
                                        )
                                    )
                                }
                            }
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = MongoDB.deleteAllNote()
                        if (result is RequestState.Success) {
                            withContext(Dispatchers.Main) {
                                onSuccess()
                            }
                        } else if (result is RequestState.Error) {
                            onError(result.error)
                        }
                    }
                }
                .addOnFailureListener {
                    onError(it)
                }
        } else {
            onError(Exception("No internet connection"))
        }
    }


    private fun observeAllNotes() {
        viewModelScope.launch {
            MongoDB.getAllNotes().collect { result ->
                notes.value = result
            }
        }
    }


}