package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;

public class InfoCommand {
    private final ClanManager clanManager;

    public InfoCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        clanManager.showInfo(player);
        return true;
    }
}