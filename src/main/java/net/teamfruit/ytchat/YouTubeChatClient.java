package net.teamfruit.ytchat;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

public class YouTubeChatClient {
    public static KeyBinding openConfig;

    public void initClient() {
        openConfig = new KeyBinding(
                I18n.format(MessageUtils.makeTranslationKey("key", new ResourceLocation(YouTubeChat.MODID, "config"))),
                Keyboard.KEY_NUMPAD9,
                YouTubeChat.APPNAME);

        ClientRegistry.registerKeyBinding(openConfig);
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) {
        if (openConfig == null)
            return;

        while (openConfig.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new YouTubeConfigurationGui(null));
        }
    }
}
