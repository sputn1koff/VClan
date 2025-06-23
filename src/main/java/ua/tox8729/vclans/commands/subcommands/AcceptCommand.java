package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class AcceptCommand {
    private final ClanManager clanManager;

    public AcceptCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendList(player, "create-help");
            return true;
        }

        String clanName = args[1];
        clanManager.acceptInvite(player, clanName);
        return true;
    }
}