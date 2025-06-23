package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;


public class BalanceCommand {
    private final ClanManager clanManager;

    public BalanceCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        clanManager.showBalance(player);
        return true;
    }
}