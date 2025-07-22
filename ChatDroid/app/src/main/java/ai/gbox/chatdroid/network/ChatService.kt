package ai.gbox.chatdroid.network

import com.squareup.moshi.Json
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatService {
    
    // Get chat list
    @GET("chats/")
    suspend fun getChats(): Response<List<ChatTitleIdResponse>>
    
    @GET("chats/list")
    suspend fun getChatList(@Query("page") page: Int? = null): Response<List<ChatTitleIdResponse>>
    
    // Get specific chat
    @GET("chats/{id}")
    suspend fun getChatById(@Path("id") id: String): Response<ChatResponse>
    
    // Create new chat
    @POST("chats/new")
    suspend fun createNewChat(@Body chatForm: ChatForm): Response<ChatResponse>
    
    // Update chat
    @POST("chats/{id}")
    suspend fun updateChat(@Path("id") id: String, @Body chatForm: ChatForm): Response<ChatResponse>
    
    // Delete chat
    @DELETE("chats/{id}")
    suspend fun deleteChat(@Path("id") id: String): Response<Boolean>
    
    // Search chats
    @GET("chats/search")
    suspend fun searchChats(
        @Query("text") text: String,
        @Query("page") page: Int? = null
    ): Response<List<ChatTitleIdResponse>>
    
    // Archive operations
    @POST("chats/{id}/archive")
    suspend fun toggleArchiveChat(@Path("id") id: String): Response<ChatResponse>
    
    @GET("chats/archived")
    suspend fun getArchivedChats(
        @Query("page") page: Int? = null
    ): Response<List<ChatTitleIdResponse>>
    
    // Pin operations
    @GET("chats/{id}/pinned")
    suspend fun getPinnedStatus(@Path("id") id: String): Response<Boolean>
    
    @POST("chats/{id}/pin")
    suspend fun togglePinChat(@Path("id") id: String): Response<ChatResponse>
    
    @GET("chats/pinned")
    suspend fun getPinnedChats(): Response<List<ChatTitleIdResponse>>
    
    // Clone chat
    @POST("chats/{id}/clone")
    suspend fun cloneChat(@Path("id") id: String, @Body cloneForm: CloneForm): Response<ChatResponse>
    
    // Share operations
    @POST("chats/{id}/share")
    suspend fun shareChat(@Path("id") id: String): Response<ChatResponse>
    
    @DELETE("chats/{id}/share")
    suspend fun unshareChat(@Path("id") id: String): Response<Boolean>
    
    @GET("chats/share/{shareId}")
    suspend fun getSharedChat(@Path("shareId") shareId: String): Response<ChatResponse>
    
    // Folder operations
    @POST("chats/{id}/folder")
    suspend fun updateChatFolder(@Path("id") id: String, @Body folderForm: ChatFolderIdForm): Response<ChatResponse>
    
    // Message operations
    @POST("chats/{id}/messages/{messageId}")
    suspend fun updateMessage(
        @Path("id") chatId: String,
        @Path("messageId") messageId: String,
        @Body messageForm: MessageForm
    ): Response<ChatResponse>
    
    // Chat completion (for sending messages)
    @POST("../chat/completions")
    suspend fun sendChatCompletion(@Body completionRequest: ChatCompletionRequest): Response<ResponseBody>
    
    // Get models
    @GET("../models")
    suspend fun getModels(): Response<ModelsResponse>
}

// Data models for API requests/responses
data class ChatTitleIdResponse(
    val id: String,
    val title: String,
    @Json(name = "updated_at") val updatedAt: Long,
    @Json(name = "created_at") val createdAt: Long,
    val archived: Boolean = false,
    val pinned: Boolean = false
)

data class ChatResponse(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val title: String,
    val chat: ChatData,
    @Json(name = "updated_at") val updatedAt: Long,
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "share_id") val shareId: String? = null,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val meta: Map<String, Any> = emptyMap(),
    @Json(name = "folder_id") val folderId: String? = null
)

data class ChatData(
    val history: ChatHistory,
    val models: List<String> = emptyList(),
    val options: Map<String, Any> = emptyMap(),
    val title: String? = null
)

data class ChatHistory(
    val currentId: String? = null,
    val messages: Map<String, Message> = emptyMap()
)

data class Message(
    val id: String,
    val parentId: String? = null,
    val childrenIds: List<String> = emptyList(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis() / 1000,
    val files: List<Any> = emptyList()
)

data class ChatForm(
    val chat: ChatData
)

data class CloneForm(
    val title: String? = null
)

data class ChatFolderIdForm(
    @Json(name = "folder_id") val folderId: String? = null
)

data class MessageForm(
    val content: String
)

// Chat completion models for sending messages
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    @Json(name = "top_p") val topP: Float? = null,
    @Json(name = "frequency_penalty") val frequencyPenalty: Float? = null,
    @Json(name = "presence_penalty") val presencePenalty: Float? = null,
    val user: String? = null
)

data class ChatMessage(
    val role: String, // "user", "assistant", "system"
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage? = null
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @Json(name = "finish_reason") val finishReason: String?
)

data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int,
    @Json(name = "completion_tokens") val completionTokens: Int,
    @Json(name = "total_tokens") val totalTokens: Int
)

// Models API
data class ModelsResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val `object`: String = "model",
    val owned_by: String? = null,
    val name: String? = null,
    val info: ModelInfoDetails? = null
)

data class ModelInfoDetails(
    val meta: ModelMeta? = null
)

data class ModelMeta(
    val description: String? = null,
    val capabilities: Map<String, Any>? = null
) 