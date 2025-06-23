package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class ChatCommand {
    private final VClans plugin;
    private final ClanManager clanManager;

    public ChatCommand(VClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "chat-empty-message");
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }
        String messageContent = message.toString().trim();

        boolean sent = clanManager.sendClanChat(player, messageContent);
        return sent;
    }
}