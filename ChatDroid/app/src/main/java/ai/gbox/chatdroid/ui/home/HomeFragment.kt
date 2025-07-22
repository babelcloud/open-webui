package ai.gbox.chatdroid.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ai.gbox.chatdroid.ChatDetailActivity
import ai.gbox.chatdroid.databinding.FragmentHomeBinding
import ai.gbox.chatdroid.network.ChatTitleIdResponse
import ai.gbox.chatdroid.repository.ChatRepository

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatListAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "Creating HomeFragment view")
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupObservers()
        setupFab()

        return root
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter(
            onChatClick = { chat ->
                // Navigate to chat detail screen
                Log.d("HomeFragment", "Chat clicked: ${chat.title}")
                openChatDetail(chat)
            },
            onChatLongClick = { chat ->
                // Show context menu for chat options
                showChatOptionsDialog(chat)
            }
        )
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter
        
        // Setup pull-to-refresh
        val swipe = binding.root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipe.setOnRefreshListener {
            Log.d("HomeFragment", "Pull-to-refresh triggered")
            homeViewModel.refreshChats()
        }
    }

    private fun setupObservers() {
        homeViewModel.chats.observe(viewLifecycleOwner) { chatList ->
            Log.d("HomeFragment", "Received chat list with ${chatList.size} items")
            adapter.update(chatList)
            updateEmptyState(chatList.isEmpty(), false, null)
        }
        
        homeViewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                Log.e("HomeFragment", "Error observed: $it")
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                updateEmptyState(true, false, "Error: $it")
            }
        }

        homeViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("HomeFragment", "Loading state: $isLoading")
            if (isLoading) {
                binding.swipeRefresh.isRefreshing = true
            }
            else {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupFab() {
        // FloatingActionButton is declared in Activity's layout
        val fab = requireActivity().findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            // Create a new chat and refresh list
            Log.d("HomeFragment", "FAB clicked: creating new chat")
            homeViewModel.createNewChat()
        }
    }

    private fun showChatOptionsDialog(chat: ChatTitleIdResponse) {
        val options = arrayOf("Open", "Delete", "Pin/Unpin", "Archive/Unarchive")
        
        AlertDialog.Builder(requireContext())
            .setTitle(chat.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openChatDetail(chat)
                    1 -> showDeleteConfirmationDialog(chat)
                    2 -> {
                        Log.d("HomeFragment", "Toggling pin for chat: ${chat.title}")
                        homeViewModel.togglePinChat(chat.id)
                    }
                    3 -> {
                        Log.d("HomeFragment", "Toggling archive for chat: ${chat.title}")
                        homeViewModel.toggleArchiveChat(chat.id)
                    }
                }
            }
            .show()
    }

    private fun openChatDetail(chat: ChatTitleIdResponse) {
        val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
            putExtra(ChatDetailActivity.EXTRA_CHAT_ID, chat.id)
            putExtra(ChatDetailActivity.EXTRA_CHAT_TITLE, chat.title)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmationDialog(chat: ChatTitleIdResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete \"${chat.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                homeViewModel.deleteChat(chat.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean, isLoading: Boolean, errorMessage: String?) {
        if (isLoading) {
            binding.tvEmptyMessage.text = "Loading chats..."
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else if (errorMessage != null) {
            binding.tvEmptyMessage.text = errorMessage
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else if (isEmpty) {
            binding.tvEmptyMessage.text = "No chats available. Start a new conversation!"
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}