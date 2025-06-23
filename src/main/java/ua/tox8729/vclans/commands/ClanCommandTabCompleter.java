package ua.tox8729.vclans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClanCommandTabCompleter implements TabCompleter {
    private final ClanManager clanManager;

    public ClanCommandTabCompleter(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();
        boolean isInClan = clanManager.getPlayerClan(player.getUniqueId()) != null;

        if (args.length == 1) {
            completions.addAll(isInClan ? Arrays.asList(
                    "delete", "invite", "accept", "leave", "kick",
                    "pvp", "invest", "withdraw", "balance", "info", "chat", "menu", "glow", "top"
            ) : Arrays.asList("create", "accept", "top"));
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            switch (subcommand) {
                case "invite":
                case "kick":
                    completions.addAll(player.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> !name.equals(player.getName()))
                            .collect(Collectors.toList()));
                    break;
                case "accept":
                    completions.addAll(clanManager.getInvitations().getOrDefault(player.getUniqueId(), new ArrayList<>()));
                    break;
                case "invest":
                case "withdraw":
                    completions.add("сумма");
                    break;
                case "top":
                    completions.addAll(Arrays.asList("money", "kills"));
                    break;
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}