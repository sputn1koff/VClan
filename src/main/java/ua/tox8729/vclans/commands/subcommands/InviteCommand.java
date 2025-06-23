package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class InviteCommand {
    private final ClanManager clanManager;

    public InviteCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendList(player, "clan-help");
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            MessageUtil.sendError(player, "player-not-found", "player", targetName);
            return true;
        }

        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return true;
        }

        clanManager.invitePlayer(player, target, clan.getName());
        return true;
    }
}