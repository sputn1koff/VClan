package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class PvpCommand {
    private final ClanManager clanManager;

    public PvpCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return true;
        }

        clanManager.togglePvp(player);
        return true;
    }
}