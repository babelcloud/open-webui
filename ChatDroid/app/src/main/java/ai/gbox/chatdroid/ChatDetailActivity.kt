package ai.gbox.chatdroid

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ai.gbox.chatdroid.databinding.ActivityChatDetailBinding

class ChatDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatDetailBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var viewModel: ChatDetailViewModel

    companion object {
        const val EXTRA_CHAT_ID = "chat_id"
        const val EXTRA_CHAT_TITLE = "chat_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d("ChatDetailActivity", "Starting onCreate")
            
            binding = ActivityChatDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val chatId = intent.getStringExtra(EXTRA_CHAT_ID)
            val chatTitle = intent.getStringExtra(EXTRA_CHAT_TITLE)

            Log.d("ChatDetailActivity", "Chat ID: $chatId, Title: $chatTitle")

            if (chatId == null) {
                Log.e("ChatDetailActivity", "No chat ID provided")
                finish()
                return
            }

            setupToolbar(chatTitle ?: "Chat")
            setupRecyclerView()
            setupViewModel(chatId)
            setupMessageInput()
            
            Log.d("ChatDetailActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error loading chat: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar(title: String) {
        try {
            Log.d("ChatDetailActivity", "Setting up toolbar")
            
            // Only set the toolbar if we don't already have an action bar
            if (supportActionBar == null) {
                setSupportActionBar(binding.toolbar)
            }
            
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setTitle(title)
            }
            
            Log.d("ChatDetailActivity", "Toolbar setup completed")
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up toolbar", e)
            
            // Fallback: just set the title directly on the toolbar
            binding.toolbar.title = title
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d("ChatDetailActivity", "Setting up RecyclerView")
            adapter = MessageAdapter()
            binding.rvMessages.apply {
                // Use default linear layout; adapter already reverses messages so newest appears first
                layoutManager = LinearLayoutManager(this@ChatDetailActivity)
                adapter = this@ChatDetailActivity.adapter
            }
            Log.d("ChatDetailActivity", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up RecyclerView", e)
            throw e
        }
    }

    private fun setupViewModel(chatId: String) {
        try {
            Log.d("ChatDetailActivity", "Setting up ViewModel")
            viewModel = ViewModelProvider(this)[ChatDetailViewModel::class.java]
            
            viewModel.messages.observe(this) { messages ->
                Log.d("ChatDetailActivity", "Received ${messages.size} messages")
                adapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(0) // Scroll to bottom (newest)
                }
            }

            viewModel.loading.observe(this) { isLoading ->
                // TODO: Show/hide loading indicator
                Log.d("ChatDetailActivity", "Loading: $isLoading")
            }

            viewModel.error.observe(this) { error ->
                error?.let {
                    Log.e("ChatDetailActivity", "Error: $it")
                    Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show()
                }
            }

            // Load the chat
            viewModel.loadChat(chatId)
            Log.d("ChatDetailActivity", "ViewModel setup completed")
        } catch (e: Exception) {
            Log.e("ChatDetailActivity", "Error setting up ViewModel", e)
            throw e
        }
    }

    private fun setupMessageInput() {
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(messageText)
                binding.etMessage.setText("")
            }
        }

        // TODO: Add typing indicators, file attachments, etc.
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}