/**
 * Copyright 2017 Google Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.teamfruit.ytchat.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;
import net.minecraft.client.Minecraft;
import net.teamfruit.ytchat.YouTubeChat;
import net.teamfruit.ytchat.api.YouTubeChatMessageListener;
import net.teamfruit.ytchat.api.YouTubeChatService;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static net.teamfruit.ytchat.MessageUtils.showErrorMessage;
import static net.teamfruit.ytchat.MessageUtils.showMessage;

/**
 * Manages connection to the YouTube chat service, posting chat messages, deleting chat messages,
 * polling for chat messages and notifying subcribers.
 */
public class ChatService implements YouTubeChatService {
    private static final String LIVE_CHAT_FIELDS =
            "items(authorDetails(channelId,displayName,isChatModerator,isChatOwner,isChatSponsor,"
                    + "profileImageUrl),snippet(displayMessage,superChatDetails,publishedAt)),"
                    + "nextPageToken,pollingIntervalMillis";
    private ExecutorService executor;
    private YouTube youtube;
    private String liveChatId;
    private boolean isInitialized;
    private List<YouTubeChatMessageListener> listeners;
    private String nextPageToken;
    private Timer pollTimer;
    private long nextPoll;

    public ChatService() {
        listeners = new ArrayList<>();
    }

    public void start(
            final String videoId,
            final String clientSecret) {
        executor = Executors.newCachedThreadPool();
        executor.execute(
                () -> {
                    try {
                        // Build auth scopes
                        List<String> scopes = new ArrayList<>();
                        scopes.add(YouTubeScopes.YOUTUBE_FORCE_SSL);
                        scopes.add(YouTubeScopes.YOUTUBE);

                        // Authorize the request
                        Credential credential = Auth.authorize(scopes, clientSecret, YouTubeChat.MODID);

                        // This object is used to make YouTube Data API requests
                        youtube =
                                new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                                        .setApplicationName(YouTubeChat.APPNAME)
                                        .build();

                        // Get the live chat id
                        String identity;
                        if (videoId != null && !videoId.isEmpty()) {
                            identity = "videoId " + videoId;
                            YouTube.Videos.List videoList = youtube.videos()
                                    .list("liveStreamingDetails")
                                    .setFields("items/liveStreamingDetails/activeLiveChatId")
                                    .setId(videoId);
                            VideoListResponse response = videoList.execute();
                            for (Video v : response.getItems()) {
                                liveChatId = v.getLiveStreamingDetails().getActiveLiveChatId();
                                if (liveChatId != null && !liveChatId.isEmpty()) {
                                    YouTubeChat.logger.info("Live chat id: " + liveChatId);
                                    break;
                                }
                            }
                        } else {
                            identity = "current user";
                            YouTube.LiveBroadcasts.List broadcastList = youtube
                                    .liveBroadcasts()
                                    .list("snippet")
                                    .setFields("items/snippet/liveChatId")
                                    .setBroadcastType("all")
                                    .setBroadcastStatus("active");
                            LiveBroadcastListResponse broadcastListResponse = broadcastList.execute();
                            for (LiveBroadcast b : broadcastListResponse.getItems()) {
                                liveChatId = b.getSnippet().getLiveChatId();
                                if (liveChatId != null && !liveChatId.isEmpty()) {
                                    YouTubeChat.logger.info("Live chat id: " + liveChatId);
                                    break;
                                }
                            }
                        }

                        if (liveChatId == null || liveChatId.isEmpty()) {
                            showErrorMessage("Could not find live chat for " + identity);
                            return;
                        }

                        // Initialize next page token
                        LiveChatMessageListResponse response = youtube
                                .liveChatMessages()
                                .list(liveChatId, "snippet")
                                .setFields("nextPageToken, pollingIntervalMillis")
                                .execute();
                        nextPageToken = response.getNextPageToken();
                        isInitialized = true;
                        if (pollTimer == null && !listeners.isEmpty()) {
                            poll(response.getPollingIntervalMillis());
                        } else {
                            nextPoll = System.currentTimeMillis() + response.getPollingIntervalMillis();
                        }
                        showMessage("YTC Service started");
                    } catch (Throwable t) {
                        showErrorMessage("Could not start YTC Service: " + t.getMessage());
                        YouTubeChat.logger.log(Level.ERROR, "Could not start YTC Service", t);
                    }
                });
    }

    public void stop() {
        stopPolling();
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        liveChatId = null;
        isInitialized = false;
        showMessage("YTC Service stopped");
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void subscribe(YouTubeChatMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            if (isInitialized && pollTimer == null) {
                poll(Math.max(0, nextPoll - System.currentTimeMillis()));
            }
        }
    }

    @Override
    public void unsubscribe(YouTubeChatMessageListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
            if (listeners.size() == 0) {
                stopPolling();
            }
        }
    }

    @Override
    /**
     * Posts a live chat message and notifies the caller of the message Id posted.
     */
    public void postMessage(final String message, final Consumer<String> onComplete) {
        if (!isInitialized) {
            onComplete.accept(null);
            return;
        }

        executor.execute(
                () -> {
                    try {
                        LiveChatMessage liveChatMessage = new LiveChatMessage();
                        LiveChatMessageSnippet snippet = new LiveChatMessageSnippet();
                        snippet.setType("textMessageEvent");
                        snippet.setLiveChatId(liveChatId);
                        LiveChatTextMessageDetails details = new LiveChatTextMessageDetails();
                        details.setMessageText(message);
                        snippet.setTextMessageDetails(details);
                        liveChatMessage.setSnippet(snippet);
                        YouTube.LiveChatMessages.Insert liveChatInsert =
                                youtube.liveChatMessages().insert("snippet", liveChatMessage);
                        LiveChatMessage response = liveChatInsert.execute();
                        final String messageId = response.getId();
                        onComplete.accept(messageId);
                    } catch (Throwable t) {
                        onComplete.accept(null);
                        showMessage(t.getMessage());
                        YouTubeChat.logger.log(Level.WARN, "Could not post message", t);
                    }
                });
    }

    public void deleteMessage(final String messageId, final Runnable onComplete) {
        if (messageId == null || messageId.isEmpty() || executor == null) {
            onComplete.run();
            return;
        }

        executor.execute(
                () -> {
                    try {
                        YouTube.LiveChatMessages.Delete liveChatDelete =
                                youtube.liveChatMessages().delete(messageId);
                        liveChatDelete.execute();
                        onComplete.run();
                    } catch (Throwable t) {
                        showMessage(t.getMessage());
                        YouTubeChat.logger.log(Level.WARN, "Could not delete message", t);
                        onComplete.run();
                    }
                });
    }

    private void poll(long delay) {
        pollTimer = new Timer();
        pollTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            // Check if game is paused
                            Minecraft mc = Minecraft.getMinecraft();
                            if (mc.isGamePaused()) {
                                poll(200);
                                return;
                            }

                            // Get chat messages from YouTube
                            YouTubeChat.logger.trace("Getting live chat messages");
                            LiveChatMessageListResponse response = youtube
                                    .liveChatMessages()
                                    .list(liveChatId, "snippet, authorDetails")
                                    .setPageToken(nextPageToken)
                                    .setFields(LIVE_CHAT_FIELDS)
                                    .execute();
                            nextPageToken = response.getNextPageToken();
                            final List<LiveChatMessage> messages = response.getItems();

                            // Broadcast message to listeners on main thread
                            for (int i = 0; i < messages.size(); i++) {
                                LiveChatMessage message = messages.get(i);
                                LiveChatMessageSnippet snippet = message.getSnippet();
                                broadcastMessage(
                                        message.getAuthorDetails(),
                                        snippet.getSuperChatDetails(),
                                        snippet.getDisplayMessage());
                            }
                            YouTubeChat.logger.trace("POLL DELAY: " + response.getPollingIntervalMillis());
                            poll(response.getPollingIntervalMillis());
                        } catch (Throwable t) {
                            showMessage(t.getMessage());
                            YouTubeChat.logger.log(Level.WARN, "Could not poll message", t);
                        }
                    }
                },
                delay);
    }

    public void broadcastMessage(
            LiveChatMessageAuthorDetails author, LiveChatSuperChatDetails details, String message) {
        for (YouTubeChatMessageListener listener : new ArrayList<>(listeners)) {
            listener.onMessageReceived(author, details, message);
        }
    }

    private void stopPolling() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
        }
    }
}
