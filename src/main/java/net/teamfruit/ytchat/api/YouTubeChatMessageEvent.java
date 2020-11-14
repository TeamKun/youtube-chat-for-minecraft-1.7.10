package net.teamfruit.ytchat.api;

import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import com.google.api.services.youtube.model.LiveChatSuperChatDetails;
import cpw.mods.fml.common.eventhandler.Event;

public class YouTubeChatMessageEvent extends Event {
    private final LiveChatMessageAuthorDetails author;
    private final LiveChatSuperChatDetails superChatDetails;
    private final String message;

    public YouTubeChatMessageEvent(LiveChatMessageAuthorDetails author,
                                   LiveChatSuperChatDetails superChatDetails,
                                   String message) {
        this.author = author;
        this.superChatDetails = superChatDetails;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public LiveChatMessageAuthorDetails getAuthor() {
        return author;
    }

    public LiveChatSuperChatDetails getSuperChatDetails() {
        return superChatDetails;
    }
}
