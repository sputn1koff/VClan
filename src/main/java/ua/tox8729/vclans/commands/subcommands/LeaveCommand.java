package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class LeaveCommand {
    private final ClanManager clanManager;

    public LeaveCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length > 1) {
            MessageUtil.sendList(player, "clan-help");
            return true;
        }

        clanManager.leaveClan(player);
        return true;
    }
}