package ua.tox8729.vclans.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import ua.tox8729.vclans.VClans;

import java.util.List;

public class MessageUtil {
    private static FileConfiguration config;
    private static VClans plugin;

    public static void init(VClans plugin) {
        MessageUtil.plugin = plugin;
        config = plugin.getConfig();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void sendError(CommandSender sender, String key, String... placeholders) {
        String message = config.getString("messages.error." + key, "Ошибка: сообщение не найдено!");
        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(HexUtil.translate(config.getString("messages.prefix", "") + "&c" + message));
    }

    public static void sendSuccess(CommandSender sender, String key, String... placeholders) {
        String message = config.getString("messages.success." + key, "Сообщение не найдено!");
        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(HexUtil.translate(config.getString("messages.prefix", "") + "&a" + message));
    }

    public static void sendPointsError(CommandSender sender, String key, String... placeholders) {
        String message = config.getString("messages.points.error." + key, "Ошибка: сообщение не найдено!");
        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(HexUtil.translate(config.getString("messages.prefix", "") + "&c" + message));
    }

    public static void sendPointsSuccess(CommandSender sender, String key, String... placeholders) {
        String message = config.getString("messages.points.success." + key, "Сообщение не найдено!");
        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(HexUtil.translate(config.getString("messages.prefix", "") + "&a" + message));
    }

    public static void sendList(CommandSender sender, String key, String... placeholders) {
        List<String> messages = config.getStringList("messages." + key);
        if (messages == null || messages.isEmpty()) {
            sendError(sender, "config-missing", "type", key);
            return;
        }
        for (String message : messages) {
            message = applyPlaceholders(message, placeholders);
            sender.sendMessage(HexUtil.translate(message));
        }
    }

    public static void sendRawList(CommandSender sender, List<String> messages) {
        for (String message : messages) {
            sender.sendMessage(HexUtil.translate(message));
        }
    }

    private static String applyPlaceholders(String message, String... placeholders) {
        if (message == null) return "";
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1] != null ? placeholders[i + 1] : "");
            }
        }
        return message;
    }
}