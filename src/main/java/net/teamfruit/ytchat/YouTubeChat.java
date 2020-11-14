package net.teamfruit.ytchat;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.teamfruit.ytchat.api.YouTubeChatMessageEvent;
import net.teamfruit.ytchat.service.ChatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(modid = YouTubeChat.MODID, name = YouTubeChat.APPNAME, guiFactory = YouTubeChat.GUI_FACTORY)
public class YouTubeChat {
    public static final String MODID = "ytchat";
    public static final String APPNAME = "YouTube Chat";
    public static final String GUI_FACTORY = "net.teamfruit.ytchat.YouTubeConfigurationGuiFactory";

    // Directly reference a log4j logger.
    public static final Logger logger = LogManager.getLogger(APPNAME);

    private static ChatService service = new ChatService();

    public static ChatService getServiceInternal() {
        return service;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        YouTubeConfiguration.init(event.getSuggestedConfigurationFile());
//        ClientCommandHandler.instance.registerCommand(new CommandYouTubeChat(service));
//        ClientCommandHandler.instance.registerCommand(new CommandChatAction(service));
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        getServiceInternal().subscribe((author, superChatDetails, message) ->
                MinecraftForge.EVENT_BUS.post(new YouTubeChatMessageEvent(author, superChatDetails, message)));

        YouTubeChatClient client = new YouTubeChatClient();
        FMLCommonHandler.instance().bus().register(client);
        MinecraftForge.EVENT_BUS.register(client);
        client.initClient();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent event) {
        if (event.modID.equalsIgnoreCase(MODID)) {
            YouTubeConfiguration.syncConfig(false);
        }
    }
}
