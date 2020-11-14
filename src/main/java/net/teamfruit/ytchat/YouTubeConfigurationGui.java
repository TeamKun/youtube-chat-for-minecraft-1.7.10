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

package net.teamfruit.ytchat;

import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import com.google.api.services.youtube.model.LiveChatSuperChatDetails;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Property;
import net.teamfruit.ytchat.api.YouTubeChatMessageListener;
import net.teamfruit.ytchat.gui.config.PressableButtonEntry;
import net.teamfruit.ytchat.service.Auth;
import net.teamfruit.ytchat.service.ChatService;

import java.io.IOException;
import java.util.stream.Collectors;

import static net.teamfruit.ytchat.MessageUtils.showErrorMessage;
import static net.teamfruit.ytchat.MessageUtils.showMessage;

/**
 * Gui configuration for YouTube Chat.
 */
public class YouTubeConfigurationGui extends GuiConfig {
    private static YouTubeChatMessageListener listener = (author, superChatDetails, message) -> {
        showMessage(message);
        if (superChatDetails != null
                && superChatDetails.getAmountMicros() != null
                && superChatDetails.getAmountMicros().longValue() > 0) {
            showMessage("Received "
                    + superChatDetails.getAmountDisplayString()
                    + " from "
                    + author.getDisplayName());
        }
    };

    public YouTubeConfigurationGui(GuiScreen parentScreen) {
        super(
                parentScreen,
                YouTubeConfiguration.getConfig().getCategoryNames().stream()
                        .map(e -> YouTubeConfiguration.getConfig().getCategory(e))
                        .map(ConfigElement::new)
                        .collect(Collectors.toList()),
                YouTubeChat.MODID,
                false,
                false,
                "YouTube Chat");
    }

    @Override
    public void initGui() {
        if (needsRefresh) {
            needsRefresh = false;
            ChatService service = YouTubeChat.getServiceInternal();
            Property author = new Property("author", "", Property.Type.STRING, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "mock.author")));
            Property message = new Property("message", "", Property.Type.STRING, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "mock.message")));
            entryList.listEntries.add(new GuiConfigEntries.StringEntry(this, entryList, new ConfigElement<>(author)));
            entryList.listEntries.add(new GuiConfigEntries.StringEntry(this, entryList, new ConfigElement<>(message)));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "mock.send")), w -> {
                // Message
                YouTubeChat.logger.info(String.format("YouTubeChatMock received %1$s from %2$s", message, author));
                LiveChatMessageAuthorDetails authorDetails = new LiveChatMessageAuthorDetails();
                authorDetails.setDisplayName(author.getString());
                authorDetails.setChannelId(author.getString());
                YouTubeChat.getServiceInternal().broadcastMessage(
                        authorDetails, new LiveChatSuperChatDetails(), message.getString());
                mc.displayGuiScreen(null);
            }));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "control.start")), w -> {
                // Start
                String clientSecret = YouTubeConfiguration.clientSecret;
                if (clientSecret == null || clientSecret.isEmpty()) {
                    showErrorMessage("No client secret configurated");
                    return;
                }
                service.start(YouTubeConfiguration.videoId, clientSecret);
                mc.displayGuiScreen(null);
            }));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "control.stop")), w -> {
                // Stop
                service.stop();
                mc.displayGuiScreen(null);
            }));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "control.logout")), w -> {
                // Logout
                service.stop();
                try {
                    Auth.clearCredentials();
                    mc.displayGuiScreen(null);
                } catch (IOException e) {
                    showErrorMessage(e.getMessage());
                }
            }));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "control.echo.start")), w -> {
                // Echo Start
                if (!service.isInitialized()) {
                    showErrorMessage("Service is not initialized");
                    return;
                }
                service.subscribe(listener);
                mc.displayGuiScreen(null);
            }));
            entryList.listEntries.add(new PressableButtonEntry(this, entryList, MessageUtils.makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "control.echo.stop")), w -> {
                // Echo Stop
                service.unsubscribe(listener);
                mc.displayGuiScreen(null);
            }));
        }
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        YouTubeConfiguration.syncConfig(false);
    }
}