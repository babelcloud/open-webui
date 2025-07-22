package ai.gbox.chatdroid.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.gbox.chatdroid.network.ChatTitleIdResponse
import ai.gbox.chatdroid.repository.ChatRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repo = ChatRepository()

    private val _chats = MutableLiveData<List<ChatTitleIdResponse>>()
    val chats: LiveData<List<ChatTitleIdResponse>> = _chats

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        Log.d("HomeViewModel", "Initializing HomeViewModel")
        loadChats()
    }

    fun loadChats() {
        Log.d("HomeViewModel", "Starting to load chats")
        _loading.postValue(true)
        viewModelScope.launch {
            try {
                val result = repo.fetchChats()
                result.fold(onSuccess = { chatList ->
                    Log.d("HomeViewModel", "Successfully loaded ${chatList.size} chats")
                    _chats.postValue(chatList)
                    _error.postValue(null)
                }, onFailure = { exception ->
                    Log.e("HomeViewModel", "Failed to load chats", exception)
                    _error.postValue("Failed to load chats: ${exception.message}")
                })
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception in loadChats", e)
                _error.postValue("Unexpected error: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun createNewChat() {
        Log.d("HomeViewModel", "Creating new chat")
        viewModelScope.launch {
            try {
                val result = repo.createNewChat()
                result.fold(onSuccess = { chatResponse ->
                    Log.d("HomeViewModel", "Successfully created new chat: ${chatResponse.id}")
                    // Refresh the chat list
                    loadChats()
                }, onFailure = { exception ->
                    Log.e("HomeViewModel", "Failed to create new chat", exception)
                    _error.postValue("Failed to create new chat: ${exception.message}")
                })
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception in createNewChat", e)
                _error.postValue("Unexpected error: ${e.message}")
            }
        }
    }

    fun deleteChat(chatId: String) {
        Log.d("HomeViewModel", "Deleting chat: $chatId")
        viewModelScope.launch {
            try {
                val result = repo.deleteChat(chatId)
                result.fold(onSuccess = { success ->
                    if (success) {
                        Log.d("HomeViewModel", "Successfully deleted chat: $chatId")
                        // Refresh the chat list
                        loadChats()
                    } else {
                        Log.e("HomeViewModel", "Failed to delete chat: $chatId")
                        _error.postValue("Failed to delete chat")
                    }
                }, onFailure = { exception ->
                    Log.e("HomeViewModel", "Failed to delete chat", exception)
                    _error.postValue("Failed to delete chat: ${exception.message}")
                })
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception in deleteChat", e)
                _error.postValue("Unexpected error: ${e.message}")
            }
        }
    }
    
    fun togglePinChat(chatId: String) {
        Log.d("HomeViewModel", "Toggling pin for chat: $chatId")
        viewModelScope.launch {
            try {
                val result = repo.togglePinChat(chatId)
                result.fold(onSuccess = {
                    Log.d("HomeViewModel", "Pin toggled for chat: ${it.id}")
                    loadChats()
                }, onFailure = { exception ->
                    Log.e("HomeViewModel", "Failed to toggle pin", exception)
                    _error.postValue("Failed to toggle pin: ${exception.message}")
                })
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception in togglePinChat", e)
                _error.postValue("Error: ${e.message}")
            }
        }
    }
    
    fun toggleArchiveChat(chatId: String) {
        Log.d("HomeViewModel", "Toggling archive for chat: $chatId")
        viewModelScope.launch {
            try {
                val result = repo.toggleArchiveChat(chatId)
                result.fold(onSuccess = {
                    Log.d("HomeViewModel", "Archive toggled for chat: ${it.id}")
                    loadChats()
                }, onFailure = { exception ->
                    Log.e("HomeViewModel", "Failed to toggle archive", exception)
                    _error.postValue("Failed to toggle archive: ${exception.message}")
                })
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Exception in toggleArchiveChat", e)
                _error.postValue("Error: ${e.message}")
            }
        }
    }

    fun refreshChats() {
        loadChats()
    }
}