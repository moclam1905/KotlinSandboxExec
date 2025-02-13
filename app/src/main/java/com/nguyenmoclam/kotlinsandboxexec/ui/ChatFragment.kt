package com.nguyenmoclam.kotlinsandboxexec.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import com.nguyenmoclam.kotlinsandboxexec.ui.ConversationAdapter
import com.nguyenmoclam.kotlinsandboxexec.viewmodel.ChatViewModel
import com.google.android.material.navigation.NavigationView
import com.nguyenmoclam.kotlinsandboxexec.R
import com.nguyenmoclam.kotlinsandboxexec.ai.AIModelFactory
import com.nguyenmoclam.kotlinsandboxexec.data.Conversation
import com.nguyenmoclam.kotlinsandboxexec.data.ConversationRepository
import com.nguyenmoclam.kotlinsandboxexec.data.MessageRepository
import com.nguyenmoclam.kotlinsandboxexec.databinding.FragmentChatWithDrawerBinding
import com.nguyenmoclam.kotlinsandboxexec.ai.OpenAIModelAdapter
import com.nguyenmoclam.kotlinsandboxexec.ai.LocalModelAdapter
import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import com.nguyenmoclam.kotlinsandboxexec.data.MessageStatus
import com.nguyenmoclam.kotlinsandboxexec.domain.model.Message

class ChatFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private var _binding: FragmentChatWithDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var messageRepository: MessageRepository
    private lateinit var conversationRepository: ConversationRepository
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatWithDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageRepository = MessageRepository(requireContext())
        conversationRepository = ConversationRepository(requireContext())
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupMessageInput()
        setupNavigationDrawer()
        initializeAIModel()
        loadSavedMessages()
    }

    private fun setupToolbar() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(binding.appToolbar)
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            if (!messages.isNullOrEmpty()) {
                adapter.submitList(messages.map { message ->
                    ChatMessage(
                        id = message.id,
                        content = message.content,
                        isFromUser = message.isFromUser,
                        codeSnippet = message.codeSnippet,
                        status = when (message.status) {
                            Message.Status.SENT -> MessageStatus.SENT
                            Message.Status.SENDING -> MessageStatus.SENDING
                            Message.Status.ERROR -> MessageStatus.ERROR
                        },
                        conversationId = message.conversationId,
                        timestamp = message.timestamp
                    )
                })
                binding.chatRecyclerView.post {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.sendButton.isEnabled = !isLoading
            binding.messageInput.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearError() // Add this method to ViewModel
            }
        }

        viewModel.currentConversationId.observe(viewLifecycleOwner) { conversationId ->
            updateConversationList()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            onCodeClick = { code ->
                // Handle code click
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, CodeEditorFragment.newInstance().apply {
                        arguments = Bundle().apply {
                            putString("code", code)
                        }
                    })
                    .addToBackStack(null)
                    .commit()
            },
            onCodeLongClick = { code ->
                // Copy code to clipboard
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("code", code)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(requireContext(), "Code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            }
        )

        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = this@ChatFragment.adapter
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(message)
                binding.messageInput.text?.clear()
                binding.messageInput.clearFocus()
                hideKeyboard()
            }
        }

        binding.messageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)
            ) {
                binding.sendButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun loadSavedMessages() {
        viewModel.loadMessages()
    }

    private fun updateApiKeyStatus() {
        val prefs = requireContext().getSharedPreferences("kotlin_sandbox_settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("openai_api_key", "") ?: ""
        binding.navigationView.menu.findItem(R.id.nav_settings).title =
            if (apiKey.isNotBlank()) "API Key: Set" else "API Key: Not Set"
    }

    private fun setupNavigationDrawer() {
        binding.navigationView.setNavigationItemSelectedListener(this)
        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).apply {
            binding.drawerLayout.addDrawerListener(this)
            syncState()
        }
        updateApiKeyStatus()
        setupConversationList()
    }

    private fun setupConversationList() {
        conversationAdapter = ConversationAdapter(
            onConversationClick = { conversation ->
                viewModel.loadConversation(conversation)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            },
            onDeleteClick = { conversation ->
                conversationRepository.deleteConversation(conversation.id)
                updateConversationList()
            }
        )
    
        binding.conversationRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = conversationAdapter
        }
    
        binding.newConversationButton.setOnClickListener {
            showNewConversationDialog()
        }
    
        updateConversationList()
    }

    private fun updateConversationList() {
        val conversations = conversationRepository.getConversations()
        conversationAdapter.submitList(conversations)
    }

    private fun showNewConversationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_conversation, null)
        val titleInput = dialogView.findViewById<android.widget.EditText>(R.id.conversationTitleInput)
        titleInput.setText("")

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("New Conversation")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val title = titleInput.text.toString()
                if (title.isNotBlank()) {
                    val conversation = Conversation(title = title)
                    conversationRepository.saveConversation(conversation)
                    updateConversationList()
                    viewModel.loadConversation(conversation)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_chat_history -> {
                updateConversationList()
            }
            R.id.nav_settings -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
            R.id.nav_clear_history -> {
                viewModel.clearChat()
                messageRepository.clearMessages()
                conversationRepository.clearConversations()
                updateConversationList()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }

    override fun onDestroyView() {
        binding.chatRecyclerView.adapter = null // Prevent memory leaks
        binding.conversationRecyclerView.adapter = null
        binding.drawerLayout.removeDrawerListener(drawerToggle)
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeAIModel() {
        val prefs = requireContext().getSharedPreferences("kotlin_sandbox_settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("openai_api_key", "") ?: ""
        
        val model = AIModelFactory.createModel(requireContext(), apiKey)
        viewModel.setAIModel(model)
    }

    companion object {
        fun newInstance() = ChatFragment()
    }
}