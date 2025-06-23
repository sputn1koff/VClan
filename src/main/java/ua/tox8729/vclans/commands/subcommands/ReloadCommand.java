package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.utils.MessageUtil;

public class ReloadCommand {
    private final VClans plugin;

    public ReloadCommand(VClans plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("vclans.reload")) {
                MessageUtil.sendError(player, "no-permission-message");
                return true;
            }
        }

        plugin.reloadPlugin();
        MessageUtil.sendSuccess(sender, "reload-success-message");
        return true;
    }
}
