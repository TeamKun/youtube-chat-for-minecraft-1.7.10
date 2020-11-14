/**
 * Copyright 2020 Kamesuta.
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

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for YouTube Chat.
 */
public class YouTubeConfiguration {
    private static Configuration config;

    public static String clientSecret;
    public static String videoId;

    public static void init(File file) {
        config = new Configuration(file);
        syncConfig(true);
    }

    public static void syncConfig(boolean load) {
        if (!config.isChild) {
            if (load) {
                config.load();
            }
        }

        config.setCategoryPropertyOrder("secret", addSecretSettings());
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static List<String> addSecretSettings() {
        Property prop;
        List<String> propOrder = new ArrayList<>();

        config.addCustomCategoryComment("secret", "The client secrets from Google API console");
        config.setCategoryLanguageKey("secret", MessageUtils.makeTranslationKey("config", new ResourceLocation(YouTubeChat.MODID, "secret")));

        prop = getProperty("secret", "Client Secret", "", "The client secret from Google API console")
                .setLanguageKey(MessageUtils.makeTranslationKey("config", new ResourceLocation(YouTubeChat.MODID, "secret.client_secret")));
        clientSecret = prop.getString();
        propOrder.add(prop.getName());

        prop = getProperty("secret", "Video ID", "", "The id of the live video")
                .setLanguageKey(MessageUtils.makeTranslationKey("config", new ResourceLocation(YouTubeChat.MODID, "secret.video_id")));
        videoId = prop.getString();
        propOrder.add(prop.getName());

        return propOrder;
    }

    public static Property getProperty(String category, String name, String defaultValue, String comment) {
        return config.get(category, name, defaultValue, comment);
    }

    public static Configuration getConfig() {
        return config;
    }
}
