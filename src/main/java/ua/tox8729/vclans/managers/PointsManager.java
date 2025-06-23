package ua.tox8729.vclans.managers;

import ua.tox8729.vclans.database.ClanDatabase;
import ua.tox8729.vclans.utils.MessageUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final ClanDatabase clanDatabase;
    private final Map<UUID, Integer> playerPoints;

    public PointsManager(ClanDatabase clanDatabase) {
        this.clanDatabase = clanDatabase;
        this.playerPoints = new HashMap<>();
        loadPoints();
    }

    private void loadPoints() {
        playerPoints.clear();
        playerPoints.putAll(clanDatabase.loadAllPoints());
    }

    public boolean givePoints(CommandSender sender, Player target, int amount) {
        if (amount <= 0) {
            MessageUtil.sendPointsError(sender, "invalid-amount");
            return false;
        }

        UUID targetId = target.getUniqueId();
        int currentPoints = playerPoints.getOrDefault(targetId, 0);
        playerPoints.put(targetId, currentPoints + amount);
        clanDatabase.savePoints(targetId, playerPoints.get(targetId));
        MessageUtil.sendPointsSuccess(sender, "points-given", "player", target.getName(), "amount", String.valueOf(amount));
        if (sender != target) {
            MessageUtil.sendPointsSuccess(target, "points-received", "amount", String.valueOf(amount));
        }
        return true;
    }

    public boolean setPoints(CommandSender sender, Player target, int amount) {
        if (amount < 0) {
            MessageUtil.sendPointsError(sender, "invalid-amount");
            return false;
        }

        UUID targetId = target.getUniqueId();
        playerPoints.put(targetId, amount);
        clanDatabase.savePoints(targetId, amount);
        if (sender != null && !sender.equals(target)) {
            MessageUtil.sendPointsSuccess(sender, "points-set", "player", target.getName(), "amount", String.valueOf(amount));
        }
        return true;
    }

    public boolean resetPoints(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        playerPoints.put(targetId, 0);
        clanDatabase.savePoints(targetId, 0);
        if (sender != null) {
            MessageUtil.sendPointsSuccess(sender, "points-reset", "player", target.getName());
        }
        return true;
    }

    public int getPoints(UUID playerId) {
        return playerPoints.getOrDefault(playerId, 0);
    }
}