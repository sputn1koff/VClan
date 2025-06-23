package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class InvestCommand {
    private final ClanManager clanManager;
    private final VClans plugin;

    public InvestCommand(ClanManager clanManager, VClans plugin) {
        this.clanManager = clanManager;
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendList(player, "clan-help");
            return true;
        }

        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                MessageUtil.sendError(player, "invalid-amount");
                return true;
            }
            if (plugin.getEconomy().getBalance(player) < amount) {
                MessageUtil.sendError(player, "not-enough-money", "amount", String.valueOf(amount));
                return true;
            }
            clanManager.invest(player, amount);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(player, "invalid-amount");
        }
        return true;
    }
}