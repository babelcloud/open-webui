package ai.gbox.chatdroid.repository

import android.util.Log
import ai.gbox.chatdroid.network.*
import ai.gbox.chatdroid.datastore.AuthPreferences
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import java.util.UUID

// Data class to hold both user and assistant messages from a send operation
data class MessagePair(
    val userMessage: Message,
    val assistantMessage: Message
)

class ChatRepository {
    private val api: ChatService by lazy { ApiClient.create(ChatService::class.java) }

    // Get all chats
    suspend fun fetchChats(): Result<List<ChatTitleIdResponse>> {
        return try {
            Log.d("ChatRepository", "Making API call to fetch chats")
            val response = api.getChats()
            
            Log.d("ChatRepository", "API Response Code: ${response.code()}")
            Log.d("ChatRepository", "API Response Message: ${response.message()}")
            
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                Log.d("ChatRepository", "API call successful, got ${list.size} chats")
                list.forEach { chat ->
                    Log.d("ChatRepository", "Chat: id=${chat.id}, title=${chat.title}")
                }
                Result.success(list)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "API returned error: ${response.code()} ${response.message()}")
                Log.e("ChatRepository", "Error body: $errorBody")
                Result.failure(Exception("API error: ${response.code()} ${response.message()}. Body: $errorBody"))
            }
        } catch (e: JsonDataException) {
            Log.e("ChatRepository", "JSON parsing error: ${e.message}", e)
            Result.failure(Exception("JSON parsing error: ${e.message}. The API response format may not match expected structure."))
        } catch (e: HttpException) {
            Log.e("ChatRepository", "HTTP error: ${e.code()} ${e.message()}", e)
            try {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("ChatRepository", "Error response body: $errorBody")
            } catch (ex: Exception) {
                Log.e("ChatRepository", "Could not read error body", ex)
            }
            Result.failure(Exception("API error: ${e.code()} ${e.message()}"))
        } catch (e: Exception) {
            Log.e("ChatRepository", "API call failed", e)
            Result.failure(e)
        }
    }

    // Get chat by ID
    suspend fun getChatById(chatId: String): Result<ChatResponse> {
        return try {
            Log.d("ChatRepository", "Fetching chat by ID: $chatId")
            val response = api.getChatById(chatId)
            
            if (response.isSuccessful) {
                val chat = response.body()
                if (chat != null) {
                    Log.d("ChatRepository", "Successfully fetched chat: ${chat.title}")
                    Result.success(chat)
                } else {
                    Log.e("ChatRepository", "Chat not found")
                    Result.failure(Exception("Chat not found"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to fetch chat: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to fetch chat: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching chat by ID", e)
            Result.failure(e)
        }
    }

    // Create new chat
    suspend fun createNewChat(title: String = "New Chat"): Result<ChatResponse> {
        return try {
            Log.d("ChatRepository", "Creating new chat with title: $title")
            
            // Create a minimal chat structure
            val chatData = ChatData(
                history = ChatHistory(
                    currentId = null,
                    messages = emptyMap()
                ),
                title = title
            )
            
            val chatForm = ChatForm(chat = chatData)
            val response = api.createNewChat(chatForm)
            
            if (response.isSuccessful) {
                val chat = response.body()
                if (chat != null) {
                    Log.d("ChatRepository", "Successfully created chat: ${chat.id}")
                    Result.success(chat)
                } else {
                    Log.e("ChatRepository", "Failed to create chat - null response")
                    Result.failure(Exception("Failed to create chat - null response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to create chat: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to create chat: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating new chat", e)
            Result.failure(e)
        }
    }

    // Send message, get AI response, and update chat
    suspend fun sendMessage(chatId: String, message: String, model: String = "gpt-3.5-turbo"): Result<MessagePair> {
        return try {
            Log.d("ChatRepository", "Sending message to chat $chatId: $message")
            
            // First, get the current chat to understand the conversation history
            val chatResult = getChatById(chatId)
            if (chatResult.isFailure) {
                return Result.failure(Exception("Failed to get chat context: ${chatResult.exceptionOrNull()?.message}"))
            }
            
            val chat = chatResult.getOrThrow()
            
            // Build conversation history for the completion request
            val conversationMessages = mutableListOf<ChatMessage>()
            
            // Add existing messages from chat history
            val history = chat.chat.history
            var currentId = history.currentId
            val visitedIds = mutableSetOf<String>()
            val messageOrder = mutableListOf<Message>()
            
            // Collect messages in reverse chronological order
            while (currentId != null && currentId !in visitedIds) {
                val historyMessage = history.messages[currentId]
                if (historyMessage != null) {
                    messageOrder.add(0, historyMessage)
                    visitedIds.add(currentId)
                    currentId = historyMessage.parentId
                } else {
                    break
                }
            }
            
            // Convert to chat messages for completion API
            messageOrder.forEach { historyMessage ->
                conversationMessages.add(ChatMessage(
                    role = historyMessage.role,
                    content = historyMessage.content
                ))
            }
            
            // Add the new user message
            conversationMessages.add(ChatMessage(role = "user", content = message))
            
            val completionRequest = ChatCompletionRequest(
                model = model,
                messages = conversationMessages,
                stream = false
            )
            
            val response = api.sendChatCompletion(completionRequest)
            
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Log.d("ChatRepository", "Received completion response: $responseBody")
                
                if (responseBody != null) {
                    // Parse the completion response to get the assistant's message
                    val assistantResponse = parseCompletionResponse(responseBody)
                    
                    if (assistantResponse != null) {
                        Log.d("ChatRepository", "Assistant response parsed successfully: $assistantResponse")
                        
                        // Create new message IDs
                        val userMessageId = UUID.randomUUID().toString()
                        val assistantMessageId = UUID.randomUUID().toString()
                        
                        Log.d("ChatRepository", "Creating messages with IDs: user=$userMessageId, assistant=$assistantMessageId")
                        
                        // Create user and assistant messages
                        val userMessage = Message(
                            id = userMessageId,
                            parentId = history.currentId,
                            role = "user",
                            content = message,
                            timestamp = System.currentTimeMillis() / 1000
                        )
                        
                        val assistantMessage = Message(
                            id = assistantMessageId,
                            parentId = userMessageId,
                            role = "assistant",
                            content = assistantResponse,
                            model = model,
                            timestamp = System.currentTimeMillis() / 1000
                        )
                        
                        // Update the chat history
                        val updatedMessages = history.messages.toMutableMap()
                        updatedMessages[userMessageId] = userMessage
                        updatedMessages[assistantMessageId] = assistantMessage
                        
                        val updatedHistory = ChatHistory(
                            currentId = assistantMessageId,
                            messages = updatedMessages
                        )
                        
                        val updatedChatData = chat.chat.copy(history = updatedHistory)
                        val chatForm = ChatForm(chat = updatedChatData)
                        
                        // Update the chat on the server
                        Log.d("ChatRepository", "Updating chat on server with chatId: $chatId")
                        val updateResult = api.updateChat(chatId, chatForm)
                        
                        if (updateResult.isSuccessful) {
                            Log.d("ChatRepository", "Successfully updated chat with new messages")
                            Log.d("ChatRepository", "Returning MessagePair with assistant content: ${assistantMessage.content}")
                            Result.success(MessagePair(userMessage, assistantMessage))
                        } else {
                            val errorBody = updateResult.errorBody()?.string()
                            Log.e("ChatRepository", "Failed to update chat: ${updateResult.code()} - ${updateResult.message()}")
                            Log.e("ChatRepository", "Update error body: $errorBody")
                            Result.failure(Exception("Failed to update chat: ${updateResult.code()} - $errorBody"))
                        }
                    } else {
                        Log.e("ChatRepository", "Could not parse assistant response from completion")
                        Log.e("ChatRepository", "Raw response was: $responseBody")
                        Result.failure(Exception("Could not parse assistant response from completion"))
                    }
                } else {
                    Result.failure(Exception("Empty response from chat completion"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to send message: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to send message: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }
    
    // Parse the chat completion response to extract the assistant's message
    private fun parseCompletionResponse(responseBody: String): String? {
        return try {
            Log.d("ChatRepository", "Parsing completion response: $responseBody")
            
            // Create Moshi instance for JSON parsing
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            
            // Try parsing as ChatCompletionResponse first
            val completionAdapter = moshi.adapter(ChatCompletionResponse::class.java)
            val completionResponse = completionAdapter.fromJson(responseBody)
            
            if (completionResponse != null && completionResponse.choices.isNotEmpty()) {
                val assistantMessage = completionResponse.choices[0].message.content
                Log.d("ChatRepository", "Successfully parsed assistant message: $assistantMessage")
                return assistantMessage
            }
            
            // If that fails, try parsing as a generic JSON with manual field extraction
            Log.d("ChatRepository", "Standard parsing failed, trying manual extraction")
            
            // Look for different possible response formats from Open WebUI
            val patterns = listOf(
                "\"content\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)",  // Standard OpenAI format
                "\"message\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)",  // Alternative format
                "\"text\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)",     // Text field format
                "\"response\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)" // Response field format
            )
            
            for (pattern in patterns) {
                val regex = pattern.toRegex()
                val match = regex.find(responseBody)
                if (match != null) {
                    val content = match.groupValues[1]
                    // Unescape JSON escape sequences
                    val unescaped = content
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                    Log.d("ChatRepository", "Extracted content using pattern: $unescaped")
                    return unescaped
                }
            }
            
            Log.w("ChatRepository", "Could not extract content from completion response with any pattern")
            Log.w("ChatRepository", "Response was: $responseBody")
            null
            
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error parsing completion response: ${e.message}", e)
            Log.e("ChatRepository", "Response body was: $responseBody")
            null
        }
    }

    // Delete chat
    suspend fun deleteChat(chatId: String): Result<Boolean> {
        return try {
            Log.d("ChatRepository", "Deleting chat: $chatId")
            val response = api.deleteChat(chatId)
            
            if (response.isSuccessful) {
                val success = response.body() ?: false
                Log.d("ChatRepository", "Delete chat result: $success")
                Result.success(success)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to delete chat: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to delete chat: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat", e)
            Result.failure(e)
        }
    }

    // Get available models
    suspend fun getModels(): Result<List<ModelInfo>> {
        return try {
            Log.d("ChatRepository", "Fetching available models")
            val response = api.getModels()
            
            if (response.isSuccessful) {
                val modelsResponse = response.body()
                val models = modelsResponse?.data ?: emptyList()
                Log.d("ChatRepository", "Successfully fetched ${models.size} models")
                Result.success(models)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to fetch models: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to fetch models: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching models", e)
            Result.failure(e)
        }
    }

    // Search chats
    suspend fun searchChats(query: String): Result<List<ChatTitleIdResponse>> {
        return try {
            Log.d("ChatRepository", "Searching chats with query: $query")
            val response = api.searchChats(query)
            
            if (response.isSuccessful) {
                val chats = response.body() ?: emptyList()
                Log.d("ChatRepository", "Search returned ${chats.size} chats")
                Result.success(chats)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to search chats: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to search chats: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error searching chats", e)
            Result.failure(e)
        }
    }

    // Toggle pin status
    suspend fun togglePinChat(chatId: String): Result<ChatResponse> {
        return try {
            Log.d("ChatRepository", "Toggling pin status for chat: $chatId")
            val response = api.togglePinChat(chatId)
            
            if (response.isSuccessful) {
                val chat = response.body()
                if (chat != null) {
                    Log.d("ChatRepository", "Successfully toggled pin status: ${chat.pinned}")
                    Result.success(chat)
                } else {
                    Result.failure(Exception("Failed to toggle pin - null response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to toggle pin: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to toggle pin: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error toggling pin status", e)
            Result.failure(e)
        }
    }

    // Toggle archive status
    suspend fun toggleArchiveChat(chatId: String): Result<ChatResponse> {
        return try {
            Log.d("ChatRepository", "Toggling archive status for chat: $chatId")
            val response = api.toggleArchiveChat(chatId)
            
            if (response.isSuccessful) {
                val chat = response.body()
                if (chat != null) {
                    Log.d("ChatRepository", "Successfully toggled archive status: ${chat.archived}")
                    Result.success(chat)
                } else {
                    Result.failure(Exception("Failed to toggle archive - null response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Failed to toggle archive: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to toggle archive: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error toggling archive status", e)
            Result.failure(e)
        }
    }
}