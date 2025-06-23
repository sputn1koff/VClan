package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.utils.MessageUtil;

public class PointsCommand {
    private final VClans plugin;

    public PointsCommand(VClans plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("vclans.points")) {
            MessageUtil.sendError(sender, "no-permission-message");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.sendPointsError(sender, "points-usage");
            return true;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("give") && !action.equals("set") && !action.equals("reset")) {
            MessageUtil.sendPointsError(sender, "points-usage");
            return true;
        }

        if (args.length < 3 && !action.equals("reset")) {
            MessageUtil.sendPointsError(sender, "points-usage");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            MessageUtil.sendPointsError(sender, "player-not-found", "player", args[2]);
            return true;
        }

        switch (action) {
            case "give":
                if (args.length < 4) {
                    MessageUtil.sendPointsError(sender, "points-usage");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[3]);
                    return plugin.getPointsManager().givePoints(sender, target, amount);
                } catch (NumberFormatException e) {
                    MessageUtil.sendPointsError(sender, "invalid-amount");
                    return true;
                }
            case "set":
                if (args.length < 4) {
                    MessageUtil.sendPointsError(sender, "points-usage");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[3]);
                    return plugin.getPointsManager().setPoints(sender, target, amount);
                } catch (NumberFormatException e) {
                    MessageUtil.sendPointsError(sender, "invalid-amount");
                    return true;
                }
            case "reset":
                return plugin.getPointsManager().resetPoints(sender, target);
            default:
                MessageUtil.sendPointsError(sender, "points-usage");
                return true;
        }
    }
}