package net.teamfruit.ytchat.api;

import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import com.google.api.services.youtube.model.LiveChatSuperChatDetails;

/**
 * Notifies subscribers of chat message details when a message is received.
 */
public interface YouTubeChatMessageListener {
    void onMessageReceived(
            LiveChatMessageAuthorDetails author,
            LiveChatSuperChatDetails superChatDetails,
            String message);
}
