package ua.tox8729.vclans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VClansCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (player.hasPermission("vclans.reload")) {
                completions.add("reload");
            }
            if (player.hasPermission("vclans.points")) {
                completions.add("points");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("points") && player.hasPermission("vclans.points")) {
            completions.add("give");
            completions.add("set");
            completions.add("reset");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("points") && player.hasPermission("vclans.points")) {
            for (Player onlinePlayer : sender.getServer().getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}