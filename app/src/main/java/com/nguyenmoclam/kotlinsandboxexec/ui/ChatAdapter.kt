package com.nguyenmoclam.kotlinsandboxexec.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nguyenmoclam.kotlinsandboxexec.R

import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import com.nguyenmoclam.kotlinsandboxexec.data.MessageStatus
import io.github.rosemoe.sora.widget.CodeEditor

class ChatAdapter(
    private val onCodeClick: (String) -> Unit,
    private val onCodeLongClick: (String) -> Unit
) : ListAdapter<ChatMessage, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = when (viewType) {
            VIEW_TYPE_USER -> R.layout.item_message_user
            else -> R.layout.item_message_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val codeEditor: CodeEditor? = itemView.findViewById(R.id.codeEditor)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)

        fun bind(message: ChatMessage) {
            messageText.text = message.content
            messageText.visibility = if (message.codeSnippet == null) View.VISIBLE else View.GONE

            codeEditor?.apply {
                visibility = if (message.codeSnippet != null) View.VISIBLE else View.GONE
                message.codeSnippet?.let { code ->
                    setText(code)
                    setEditable(false)
                    setOnClickListener { onCodeClick(code) }
                    setOnLongClickListener {
                        onCodeLongClick(code)
                        true
                    }
                }
            }

            // Handle message status with improved visibility
            statusText.visibility = when (message.status) {
                MessageStatus.SENDING, MessageStatus.ERROR -> View.VISIBLE
                else -> View.GONE
            }
            statusText.text = when (message.status) {
                MessageStatus.SENDING -> itemView.context.getString(R.string.running)
                MessageStatus.ERROR -> itemView.context.getString(R.string.execution_error, "Tap to retry")
                else -> ""
            }
            statusText.setTextColor(when (message.status) {
                MessageStatus.ERROR -> itemView.context.getColor(android.R.color.holo_red_light)
                else -> itemView.context.getColor(android.R.color.darker_gray)
            })

            // Apply different styles for user and bot messages
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
            if (message.isFromUser) {
                layoutParams.marginStart = 100
                layoutParams.marginEnd = 20
                messageText.setBackgroundResource(R.drawable.bg_user_message)
            } else {
                layoutParams.marginStart = 20
                layoutParams.marginEnd = 100
                messageText.setBackgroundResource(R.drawable.bg_bot_message)
            }
            itemView.layoutParams = layoutParams
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }
}