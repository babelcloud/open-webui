package ai.gbox.chatdroid

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ai.gbox.chatdroid.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<MessageItem> = emptyList()

    fun updateMessages(newMessages: List<MessageItem>) {
        // Reverse list order so newest messages appear at top
        messages = newMessages.reversed()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: MessageItem) {
            binding.tvMessage.text = message.content
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date(message.timestamp * 1000)
            binding.tvTimestamp.text = dateFormat.format(date)
            
            // Style based on message role
            when (message.role) {
                "user" -> styleAsUserMessage()
                "assistant" -> styleAsAssistantMessage()
                "system" -> styleAsSystemMessage()
                else -> styleAsAssistantMessage() // Default
            }
            
            // Show loading indicator if needed
            if (message.isLoading) {
                binding.tvMessage.text = "Typing..."
                binding.tvMessage.alpha = 0.7f
            } else {
                binding.tvMessage.alpha = 1.0f
            }
        }
        
        private fun styleAsUserMessage() {
            val context = binding.root.context
            
            // Align to right
            binding.messageContainer.gravity = Gravity.END
            
            // User message styling (blue background, white text)
            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.purple_500)
            )
            binding.tvMessage.setTextColor(
                ContextCompat.getColor(context, android.R.color.white)
            )
            binding.tvTimestamp.setTextColor(
                ContextCompat.getColor(context, android.R.color.white)
            )
            
            // Adjust margins for right alignment
            val layoutParams = binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(
                dpToPx(48), // left margin (more space on left)
                dpToPx(4),  // top margin
                dpToPx(16), // right margin (less space on right)
                dpToPx(4)   // bottom margin
            )
        }
        
        private fun styleAsAssistantMessage() {
            val context = binding.root.context
            
            // Align to left
            binding.messageContainer.gravity = Gravity.START
            
            // Assistant message styling (light gray background, dark text)
            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.background_light)
            )
            binding.tvMessage.setTextColor(
                ContextCompat.getColor(context, android.R.color.black)
            )
            binding.tvTimestamp.setTextColor(
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )
            
            // Adjust margins for left alignment
            val layoutParams = binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(
                dpToPx(16), // left margin (less space on left)
                dpToPx(4),  // top margin
                dpToPx(48), // right margin (more space on right)
                dpToPx(4)   // bottom margin
            )
        }
        
        private fun styleAsSystemMessage() {
            val context = binding.root.context
            
            // Center align
            binding.messageContainer.gravity = Gravity.CENTER
            
            // System message styling (very light background, italic text)
            binding.messageCard.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.background_light)
            )
            binding.tvMessage.setTextColor(
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )
            binding.tvTimestamp.setTextColor(
                ContextCompat.getColor(context, android.R.color.darker_gray)
            )
            
            // Adjust margins for center alignment
            val layoutParams = binding.messageCard.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(
                dpToPx(32), // left margin
                dpToPx(4),  // top margin
                dpToPx(32), // right margin
                dpToPx(4)   // bottom margin
            )
        }
        
        private fun dpToPx(dp: Int): Int {
            val density = binding.root.context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
}