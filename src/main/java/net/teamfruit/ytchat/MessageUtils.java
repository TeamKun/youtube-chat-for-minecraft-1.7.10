package net.teamfruit.ytchat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class MessageUtils {

    public static void showMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        ICommandSender sender = mc.thePlayer;
        if (sender != null)
            sender.addChatMessage(new ChatComponentText(message));
    }

    public static void showErrorMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        ICommandSender sender = mc.thePlayer;
        if (sender != null)
            sender.addChatMessage(new ChatComponentText(message));
        else
            mc.displayGuiScreen(new GuiDisconnected(mc.currentScreen, makeTranslationKey("gui", new ResourceLocation(YouTubeChat.MODID, "error")), new ChatComponentText(message)));
    }

    public static String makeTranslationKey(String type, @Nullable ResourceLocation id) {
        return id == null ? type + ".unregistered_sadface" : type + '.' + id.getResourceDomain() + '.' + id.getResourcePath().replace('/', '.');
    }

}
