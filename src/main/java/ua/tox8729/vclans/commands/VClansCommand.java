package ua.tox8729.vclans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.commands.subcommands.PointsCommand;
import ua.tox8729.vclans.commands.subcommands.ReloadCommand;
import ua.tox8729.vclans.utils.MessageUtil;

public class VClansCommand implements CommandExecutor {
    private final VClans plugin;

    public VClansCommand(VClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.sendError(sender, "usage-message");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "reload":
                return new ReloadCommand(plugin).execute(sender, args);
            case "points":
                return new PointsCommand(plugin).execute(sender, args);
            default:
                MessageUtil.sendError(sender, "usage-message");
                return true;
        }
    }
}