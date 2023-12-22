package com.ghostchu.plugins.riaeew.text;

import com.ghostchu.plugins.riaeew.RIAEEW;
import com.ghostchu.plugins.riaeew.util.Util;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TextManager {
    private final File file;
    private final RIAEEW plugin;
    private YamlConfiguration config;
    private MiniMessage miniMessage;

    public TextManager(RIAEEW plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        init();
    }

    private void init() {
        this.config = YamlConfiguration.loadConfiguration(file);
        this.miniMessage = MiniMessage.miniMessage();

    }


    public Text of(CommandSource sender, String key, Object... args) {
        return new Text(sender, Util.fillArgs(miniMessage.deserialize(config.getString(key, "Missing no: " + key)), convert(args)));
    }

    public Text of(String key, Object... args) {
        return new Text(null, Util.fillArgs(miniMessage.deserialize(config.getString(key, "Missing no: " + key)), convert(args)));
    }

    @NotNull
    public Component[] convert(@Nullable Object... args) {
        if (args == null || args.length == 0) {
            return new Component[0];
        }
        Component[] components = new Component[args.length];
        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            if (obj == null) {
                components[i] = Component.text("null");
                continue;
            }
            Class<?> clazz = obj.getClass();
            if (obj instanceof Component) {
                Component component = (Component) obj;
                components[i] = component;
                continue;
            }
            if (obj instanceof ComponentLike) {
                ComponentLike componentLike = (ComponentLike) obj;
                components[i] = componentLike.asComponent();
                continue;
            }
            // Check
            try {
                if (Character.class.equals(clazz)) {
                    components[i] = Component.text((char) obj);
                    continue;
                }
                if (Byte.class.equals(clazz)) {
                    components[i] = Component.text((Byte) obj);
                    continue;
                }
                if (Integer.class.equals(clazz)) {
                    components[i] = Component.text((Integer) obj);
                    continue;
                }
                if (Long.class.equals(clazz)) {
                    components[i] = Component.text((Long) obj);
                    continue;
                }
                if (Float.class.equals(clazz)) {
                    components[i] = Component.text((Float) obj);
                    continue;
                }
                if (Double.class.equals(clazz)) {
                    components[i] = Component.text((Double) obj);
                    continue;
                }
                if (Boolean.class.equals(clazz)) {
                    components[i] = Component.text((Boolean) obj);
                    continue;
                }
                if (String.class.equals(clazz)) {
                    components[i] = LegacyComponentSerializer.legacySection().deserialize((String) obj);
                    continue;
                }
                if (Text.class.equals(clazz)) {
                    components[i] = ((Text) obj).component();
                }
                components[i] = LegacyComponentSerializer.legacySection().deserialize(obj.toString());
            } catch (Exception exception) {
                components[i] = LegacyComponentSerializer.legacySection().deserialize(obj.toString());
            }
            // undefined

        }
        return components;
    }

    public static class Text {
        private final Component component;
        private final CommandSource sender;

        public Text(CommandSource sender, Component component) {
            this.sender = sender;
            this.component = component.compact();
        }


        public Component component() {
            return this.component;
        }

        public String plain() {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }

        public CommandSource sender() {
            return this.sender;
        }

        public void send() {
            if (this.sender != null) {
                this.sender.sendMessage(component);
            }
        }

    }
}
