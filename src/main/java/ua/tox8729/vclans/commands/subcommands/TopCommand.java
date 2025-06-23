package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.ClanManager.Clan;
import ua.tox8729.vclans.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class TopCommand {
    private final ClanManager clanManager;

    public TopCommand(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "invalid-usage-top");
            return false;
        }

        String type = args[1].toLowerCase();
        List<Clan> topClans;
        String configKey;

        switch (type) {
            case "money":
                topClans = clanManager.getTopClansByMoney();
                configKey = "clan-top-money";
                break;
            case "kills":
                topClans = clanManager.getTopClansByKills();
                configKey = "clan-top-kills";
                break;
            default:
                MessageUtil.sendError(player, "invalid-usage-top");
                return false;
        }

        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String clanNameKey = "clan_name_" + (i + 1);
            String valueKey = "value_" + (i + 1);

            if (i < topClans.size()) {
                Clan clan = topClans.get(i);
                placeholders.add(clanNameKey);
                placeholders.add(clan.getName());
                placeholders.add(valueKey);
                placeholders.add(type.equals("money") ? String.format("%.0f", clan.getBalance()) : String.valueOf(clan.getKills()));
            } else {
                placeholders.add(clanNameKey);
                placeholders.add("-");
                placeholders.add(valueKey);
                placeholders.add("0");
            }
        }

        MessageUtil.sendList(player, configKey, placeholders.toArray(new String[0]));
        return true;
    }
}