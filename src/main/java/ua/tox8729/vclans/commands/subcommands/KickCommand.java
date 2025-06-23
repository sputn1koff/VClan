package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class KickCommand {
    private final ClanManager clanManager;

    public KickCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendList(player, "clan-help");
            return true;
        }

        String targetName = args[1];
        clanManager.kickPlayer(player, targetName);
        return true;
    }
}