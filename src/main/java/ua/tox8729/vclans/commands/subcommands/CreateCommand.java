package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

import java.util.List;

public class CreateCommand {
    private final ClanManager clanManager;
    private final VClans plugin;

    public CreateCommand(ClanManager clanManager, VClans plugin) {
        this.clanManager = clanManager;
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendList(player, "create-help");
            return true;
        }

        String clanName = args[1];
        List<String> blockedNames = clanManager.getBlockedNames();

        int minLength = plugin.getConfig().getInt("settings.clan-name-min", 3);
        int maxLength = plugin.getConfig().getInt("settings.clan-name-max", 16);
        if (clanName.length() < minLength || clanName.length() > maxLength) {
            MessageUtil.sendError(player, "invalid-clan-name-length",
                    "min", String.valueOf(minLength),
                    "max", String.valueOf(maxLength));
            return true;
        }

        String clanNameLower = clanName.toLowerCase();
        for (String blockedName : blockedNames) {
            if (clanNameLower.contains(blockedName.toLowerCase())) {
                MessageUtil.sendError(player, "clan-name-blocked", "clan", clanName);
                return true;
            }
        }

        double cost = plugin.getConfig().getDouble("settings.create-cost", 100.0);
        if (plugin.getEconomy().getBalance(player) < cost) {
            MessageUtil.sendError(player, "not-enough-money", "amount", String.valueOf(cost));
            return true;
        }

        return clanManager.createClan(player, clanName);
    }
}