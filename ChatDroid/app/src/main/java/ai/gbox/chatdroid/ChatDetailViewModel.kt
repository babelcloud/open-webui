package ai.gbox.chatdroid

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.gbox.chatdroid.network.ChatResponse
import ai.gbox.chatdroid.network.Message
import ai.gbox.chatdroid.repository.ChatRepository
import kotlinx.coroutines.launch
import java.util.UUID

class ChatDetailViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _chat = MutableLiveData<ChatResponse>()
    val chat: LiveData<ChatResponse> = _chat

    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> = _messages

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentChatId: String? = null

    fun loadChat(chatId: String) {
        currentChatId = chatId
        _loading.postValue(true)
        
        viewModelScope.launch {
            try {
                Log.d("ChatDetailViewModel", "Loading chat: $chatId")
                val result = repository.getChatById(chatId)
                
                result.fold(
                    onSuccess = { chatResponse ->
                        Log.d("ChatDetailViewModel", "Successfully loaded chat: ${chatResponse.title}")
                        _chat.postValue(chatResponse)
                        parseAndDisplayMessages(chatResponse)
                        _error.postValue(null)
                    },
                    onFailure = { exception ->
                        Log.e("ChatDetailViewModel", "Failed to load chat", exception)
                        _error.postValue("Failed to load chat: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Exception loading chat", e)
                _error.postValue("Unexpected error: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun parseAndDisplayMessages(chatResponse: ChatResponse) {
        val messageItems = mutableListOf<MessageItem>()
        val history = chatResponse.chat.history
        
        // Start from the current message and traverse the conversation tree
        var currentId = history.currentId
        val visitedIds = mutableSetOf<String>()
        
        while (currentId != null && currentId !in visitedIds) {
            val message = history.messages[currentId]
            if (message != null) {
                messageItems.add(0, MessageItem.fromMessage(message)) // Add to beginning for correct order
                visitedIds.add(currentId)
                currentId = message.parentId
            } else {
                break
            }
        }
        
        Log.d("ChatDetailViewModel", "Parsed ${messageItems.size} messages from chat history")
        _messages.postValue(messageItems)
    }

    fun sendMessage(content: String) {
        val chatId = currentChatId ?: return
        
        _loading.postValue(true)
        
        viewModelScope.launch {
            try {
                Log.d("ChatDetailViewModel", "Sending message: $content")
                
                // Add user message to UI immediately
                val userMessage = MessageItem(
                    id = UUID.randomUUID().toString(),
                    role = "user",
                    content = content,
                    timestamp = System.currentTimeMillis() / 1000,
                    isLoading = false
                )
                
                val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
                currentMessages.add(userMessage)
                _messages.postValue(currentMessages)
                
                // Send to API
                val result = repository.sendMessage(chatId, content)
                
                result.fold(
                    onSuccess = { response ->
                        Log.d("ChatDetailViewModel", "Message sent successfully")
                        // TODO: Parse the API response and add assistant message
                        // For now, just reload the chat to get the updated conversation
                        loadChat(chatId)
                    },
                    onFailure = { exception ->
                        Log.e("ChatDetailViewModel", "Failed to send message", exception)
                        _error.postValue("Failed to send message: ${exception.message}")
                        
                        // Remove the user message from UI since it failed
                        val updatedMessages = _messages.value?.toMutableList() ?: mutableListOf()
                        updatedMessages.removeLastOrNull()
                        _messages.postValue(updatedMessages)
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Exception sending message", e)
                _error.postValue("Unexpected error: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun refreshChat() {
        currentChatId?.let { loadChat(it) }
    }
}

// Data class for displaying messages in the UI
data class MessageItem(
    val id: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long,
    val isLoading: Boolean = false,
    val model: String? = null
) {
    companion object {
        fun fromMessage(message: Message): MessageItem {
            return MessageItem(
                id = message.id,
                role = message.role,
                content = message.content,
                timestamp = message.timestamp,
                model = message.model
            )
        }
    }
}