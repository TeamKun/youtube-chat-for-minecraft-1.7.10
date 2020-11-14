package net.teamfruit.ytchat.api;

import net.teamfruit.ytchat.YouTubeChat;

public class YouTubeChatAPI {
    public static YouTubeChatService getService() {
        return YouTubeChat.getServiceInternal();
    }
}
