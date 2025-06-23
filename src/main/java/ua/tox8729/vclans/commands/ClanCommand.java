package ua.tox8729.vclans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.commands.subcommands.*;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.MessageUtil;

public class ClanCommand implements CommandExecutor {
    private final ClanManager clanManager;
    private final VClans plugin;

    public ClanCommand(ClanManager clanManager, VClans plugin) {
        this.clanManager = clanManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning("Команда доступна только игрокам!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            MessageUtil.sendList(player, clanManager.getPlayerClan(player.getUniqueId()) == null ? "create-help" : "clan-help");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "create":
                return new CreateCommand(clanManager, plugin).execute(player, args);
            case "delete":
                return new DeleteCommand(clanManager).execute(player, args);
            case "invite":
                return new InviteCommand(clanManager).execute(player, args);
            case "accept":
                return new AcceptCommand(clanManager).execute(player, args);
            case "leave":
                return new LeaveCommand(clanManager).execute(player, args);
            case "kick":
                return new KickCommand(clanManager).execute(player, args);
            case "pvp":
                return new PvpCommand(clanManager).execute(player, args);
            case "invest":
                return new InvestCommand(clanManager, plugin).execute(player, args);
            case "withdraw":
                return new WithdrawCommand(clanManager, plugin).execute(player, args);
            case "balance":
                return new BalanceCommand(clanManager).execute(player, args);
            case "info":
                return new InfoCommand(clanManager).execute(player, args);
            case "chat":
                return new ChatCommand(plugin).execute(player, args);
            case "menu":
                plugin.getMenuManager().getMainMenu().openMenu(player);
                return true;
            case "glow":
                return new GlowCommand(clanManager, plugin).execute(player, args);
            case "top":
                return new TopCommand(clanManager).execute(player, args);
            default:
                MessageUtil.sendList(player, clanManager.getPlayerClan(player.getUniqueId()) == null ? "create-help" : "clan-help");
                return true;
        }
    }
}