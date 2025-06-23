package ua.tox8729.vclans.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.PointsManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VClansPlaceholderExpansion extends PlaceholderExpansion {
    private final ClanManager clanManager;
    private final PointsManager pointsManager;

    public VClansPlaceholderExpansion(ClanManager clanManager, PointsManager pointsManager) {
        this.clanManager = clanManager;
        this.pointsManager = pointsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vclans";
    }

    @Override
    public @NotNull String getAuthor() {
        return "tox8729";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null && !identifier.startsWith("money_top_") && !identifier.startsWith("kills_top_") &&
                !identifier.startsWith("money_name_top_") && !identifier.startsWith("kills_name_top_")) {
            return "";
        }

        if (identifier.equalsIgnoreCase("points")) {
            return String.valueOf(pointsManager.getPoints(player.getUniqueId()));
        }

        ClanManager.Clan clan = clanManager.getPlayerClan(player != null ? player.getUniqueId() : null);
        if (identifier.equalsIgnoreCase("name")) {
            return clan != null ? clan.getName() : "";
        } else if (identifier.equalsIgnoreCase("balance")) {
            return clan != null ? String.format("%.0f", clan.getBalance()) : "0";
        }

        if (identifier.startsWith("money_top_")) {
            try {
                int position = Integer.parseInt(identifier.replace("money_top_", ""));
                if (position < 1 || position > 10) return "0";
                List<ClanManager.Clan> topClans = clanManager.getTopClansByMoney();
                return position <= topClans.size() ? String.format("%.0f", topClans.get(position - 1).getBalance()) : "0";
            } catch (NumberFormatException e) {
                return "0";
            }
        } else if (identifier.startsWith("kills_top_")) {
            try {
                int position = Integer.parseInt(identifier.replace("kills_top_", ""));
                if (position < 1 || position > 10) return "0";
                List<ClanManager.Clan> topClans = clanManager.getTopClansByKills();
                return position <= topClans.size() ? String.valueOf(topClans.get(position - 1).getKills()) : "0";
            } catch (NumberFormatException e) {
                return "0";
            }
        } else if (identifier.startsWith("money_name_top_")) {
            try {
                int position = Integer.parseInt(identifier.replace("money_name_top_", ""));
                if (position < 1 || position > 10) return "";
                List<ClanManager.Clan> topClans = clanManager.getTopClansByMoney();
                return position <= topClans.size() ? topClans.get(position - 1).getName() : "--";
            } catch (NumberFormatException e) {
                return "";
            }
        } else if (identifier.startsWith("kills_name_top_")) {
            try {
                int position = Integer.parseInt(identifier.replace("kills_name_top_", ""));
                if (position < 1 || position > 10) return "";
                List<ClanManager.Clan> topClans = clanManager.getTopClansByKills();
                return position <= topClans.size() ? topClans.get(position - 1).getName() : "--";
            } catch (NumberFormatException e) {
                return "";
            }
        }

        return null;
    }
}