package ua.tox8729.vclans.managers;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.database.ClanDatabase;
import ua.tox8729.vclans.utils.ClanGlow;
import ua.tox8729.vclans.utils.MessageUtil;
import ua.tox8729.vclans.utils.HexUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ClanManager {
    private final ClanDatabase clanDatabase;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerToClan;
    private final Map<UUID, List<String>> invitations;
    private final VClans plugin;
    private final ClanGlow clanGlow;
    private List<String> blockedNames;
    private List<Clan> topClansByMoney;
    private List<Clan> topClansByKills;
    private long lastCacheUpdate;

    public ClanManager(ClanDatabase clanDatabase, VClans plugin) {
        this.clanDatabase = clanDatabase;
        this.clans = new HashMap<>();
        this.playerToClan = new HashMap<>();
        this.invitations = new HashMap<>();
        this.plugin = plugin;
        this.clanGlow = new ClanGlow(clanDatabase);
        this.topClansByMoney = new ArrayList<>();
        this.topClansByKills = new ArrayList<>();
        this.lastCacheUpdate = 0;
        reloadBlockedNames();
        loadClans();
        updateTopClans();
    }

    public void reloadBlockedNames() {
        File blockedNamesFile = new File(plugin.getDataFolder(), "blocking-name.yml");
        if (!blockedNamesFile.exists()) {
            plugin.saveResource("blocking-name.yml", false);
        }
        FileConfiguration blockedNamesConfig = YamlConfiguration.loadConfiguration(blockedNamesFile);
        this.blockedNames = blockedNamesConfig.getStringList("blocked-names");
    }

    public List<String> getBlockedNames() {
        return Collections.unmodifiableList(blockedNames);
    }

    private void loadClans() {
        clans.clear();
        playerToClan.clear();
        for (Clan clan : clanDatabase.loadAllClans()) {
            clans.put(clan.getName(), clan);
            for (UUID memberId : clan.getMembers()) {
                playerToClan.put(memberId, clan.getName());
            }
        }
        updateTopClans();
    }

    public void updateTopClans() {
        List<Clan> allClans = new ArrayList<>(clans.values());

        topClansByMoney = allClans.stream()
                .sorted((c1, c2) -> Double.compare(c2.getBalance(), c1.getBalance()))
                .limit(10)
                .collect(Collectors.toList());

        topClansByKills = allClans.stream()
                .sorted((c1, c2) -> Integer.compare(c2.getKills(), c1.getKills()))
                .limit(10)
                .collect(Collectors.toList());

        this.lastCacheUpdate = System.currentTimeMillis();
    }

    private void checkAndUpdateTopClans() {
        if (System.currentTimeMillis() - lastCacheUpdate > 300_000) {
            updateTopClans();
        }
    }

    public List<Clan> getTopClansByMoney() {
        checkAndUpdateTopClans();
        return Collections.unmodifiableList(topClansByMoney);
    }

    public List<Clan> getTopClansByKills() {
        checkAndUpdateTopClans();
        return Collections.unmodifiableList(topClansByKills);
    }

    public boolean createClan(Player leader, String clanName) {
        int minLength = plugin.getConfig().getInt("settings.clan-name-min", 3);
        int maxLength = plugin.getConfig().getInt("settings.clan-name-max", 16);
        if (clanName.length() < minLength || clanName.length() > maxLength) {
            MessageUtil.sendError(leader, "invalid-clan-name-length",
                    "min", String.valueOf(minLength),
                    "max", String.valueOf(maxLength));
            return false;
        }

        if (clans.containsKey(clanName) || clanName.trim().isEmpty()) {
            MessageUtil.sendError(leader, "clan-exists", "clan", clanName);
            return false;
        }
        if (playerToClan.containsKey(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "player-already-in-clan");
            return false;
        }
        if (blockedNames.stream().anyMatch(blocked -> clanName.toLowerCase().contains(blocked.toLowerCase()))) {
            MessageUtil.sendError(leader, "clan-name-blocked", "clan", clanName);
            return false;
        }

        double cost = plugin.getConfig().getDouble("settings.create-cost", 100.0);
        if (plugin.getEconomy().getBalance(leader) < cost) {
            MessageUtil.sendError(leader, "not-enough-money", "amount", String.valueOf(cost));
            return false;
        }

        EconomyResponse response = plugin.getEconomy().withdrawPlayer(leader, cost);
        if (!response.transactionSuccess()) {
            MessageUtil.sendError(leader, "not-enough-money", "amount", String.valueOf(cost));
            return false;
        }

        Clan clan = new Clan(clanName, leader.getUniqueId());
        clans.put(clanName, clan);
        playerToClan.put(leader.getUniqueId(), clanName);
        clanDatabase.saveClan(clan);
        MessageUtil.sendSuccess(leader, "clan-created", "clan", clanName);
        clanGlow.changeForPlayer(leader, this);
        updateTopClans();
        return true;
    }

    public boolean invitePlayer(Player leader, Player target, String clanName) {
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }
        if (playerToClan.containsKey(target.getUniqueId())) {
            MessageUtil.sendError(leader, "target-already-in-clan");
            return false;
        }
        int maxMembers = plugin.getConfig().getInt("settings.max-members", 6);
        if (clan.getMembers().size() >= maxMembers) {
            MessageUtil.sendError(leader, "clan-full",
                    "min_player", String.valueOf(clan.getMembers().size()),
                    "max_player", String.valueOf(maxMembers));
            return false;
        }

        invitations.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(clanName);
        clanDatabase.saveInvitation(target.getUniqueId(), clanName);
        MessageUtil.sendList(target, "clan-invite", "clan", clanName);
        MessageUtil.sendSuccess(leader, "player-invited", "player", target.getName());
        return true;
    }

    public boolean acceptInvite(Player player, String clanName) {
        List<String> invites = invitations.get(player.getUniqueId());
        if (invites == null || !invites.contains(clanName)) {
            MessageUtil.sendError(player, "no-invitation", "clan", clanName);
            return false;
        }

        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return false;
        }
        int maxMembers = plugin.getConfig().getInt("settings.max-members", 6);
        if (clan.getMembers().size() >= maxMembers) {
            MessageUtil.sendError(player, "clan-full-invite", "clan", clanName);
            invites.remove(clanName);
            clanDatabase.removeInvitation(player.getUniqueId(), clanName);
            return false;
        }

        clan.getMembers().add(player.getUniqueId());
        playerToClan.put(player.getUniqueId(), clanName);
        clanDatabase.saveClan(clan);
        invites.remove(clanName);
        clanDatabase.removeInvitation(player.getUniqueId(), clanName);
        MessageUtil.sendSuccess(player, "clan-joined", "clan", clanName);

        for (UUID memberId : clan.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                MessageUtil.sendSuccess(member, "player-joined", "player", player.getName());
                clanGlow.changeForPlayer(member, this);
            }
        }
        clanGlow.changeForPlayer(player, this);
        return true;
    }

    public boolean deleteClan(Player leader, String clanName) {
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }

        for (UUID memberId : clan.getMembers()) {
            playerToClan.remove(memberId);
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                clanGlow.resetGlowForPlayer(member);
            }
        }
        clans.remove(clanName);
        clanDatabase.deleteClan(clanName);
        MessageUtil.sendSuccess(leader, "clan-deleted", "clan", clanName);
        updateTopClans();
        return true;
    }

    public boolean leaveClan(Player player) {
        String clanName = playerToClan.get(player.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return false;
        }
        if (clan.getLeader().equals(player.getUniqueId())) {
            MessageUtil.sendError(player, "cannot-leave-as-leader");
            return false;
        }

        clan.getMembers().remove(player.getUniqueId());
        playerToClan.remove(player.getUniqueId());
        clanDatabase.saveClan(clan);
        clanDatabase.removePlayer(player.getUniqueId());
        MessageUtil.sendSuccess(player, "clan-left", "clan", clan.getName());
        clanGlow.resetGlowForPlayer(player);
        updateGlowForAllMembers(clan);
        updateTopClans();
        return true;
    }

    public boolean kickPlayer(Player leader, String targetName) {
        String clanName = playerToClan.get(leader.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(leader, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore()) {
            MessageUtil.sendError(leader, "player-not-found", "player", targetName);
            return false;
        }
        UUID targetId = target.getUniqueId();
        if (targetId.equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "cannot-kick-self");
            return false;
        }
        if (!clan.getMembers().contains(targetId)) {
            MessageUtil.sendError(leader, "player-not-in-clan", "player", targetName);
            return false;
        }

        clan.getMembers().remove(targetId);
        playerToClan.remove(targetId);
        clanDatabase.saveClan(clan);
        clanDatabase.removePlayer(targetId);
        MessageUtil.sendSuccess(leader, "player-kicked", "player", targetName);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            MessageUtil.sendError(onlineTarget, "kicked-from-clan", "clan", clanName);
            clanGlow.resetGlowForPlayer(onlineTarget);
        }
        updateGlowForAllMembers(clan);
        updateTopClans();
        return true;
    }

    public boolean togglePvp(Player leader) {
        String clanName = playerToClan.get(leader.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(leader, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }

        clan.setPvpEnabled(!clan.isPvpEnabled());
        clanDatabase.saveClan(clan);
        MessageUtil.sendSuccess(leader, clan.isPvpEnabled() ? "pvp-enabled" : "pvp-disabled");
        return true;
    }

    public boolean toggleGlow(Player leader) {
        String clanName = playerToClan.get(leader.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(leader, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }

        clan.setGlowEnabled(!clan.isGlowEnabled());
        clanDatabase.saveClan(clan);
        MessageUtil.sendSuccess(leader, clan.isGlowEnabled() ? "glow-enabled" : "glow-disabled");
        updateGlowForAllMembers(clan);
        return true;
    }

    public boolean setGlowColor(Player leader, String colorString) {
        String clanName = playerToClan.get(leader.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(leader, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(leader, "clan-not-found", "clan", clanName);
            return false;
        }
        if (!clan.getLeader().equals(leader.getUniqueId())) {
            MessageUtil.sendError(leader, "not-leader");
            return false;
        }

        try {
            String[] rgb = colorString.split(",");
            Color color = Color.fromRGB(
                    Integer.parseInt(rgb[0].trim()),
                    Integer.parseInt(rgb[1].trim()),
                    Integer.parseInt(rgb[2].trim())
            );
            clan.setGlowColor(color);
            clanDatabase.saveClan(clan);
            MessageUtil.sendSuccess(leader, "glow-color-changed");
            updateGlowForAllMembers(clan);
            return true;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            MessageUtil.sendError(leader, "invalid-color", "color", colorString);
            return false;
        }
    }

    private void updateGlowForAllMembers(Clan clan) {
        for (UUID memberId : clan.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                clanGlow.changeForPlayer(member, this);
            }
        }
    }

    public boolean invest(Player player, double amount) {
        if (amount <= 0) {
            MessageUtil.sendError(player, "invalid-amount");
            return false;
        }
        String clanName = playerToClan.get(player.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return false;
        }
        if (plugin.getEconomy().getBalance(player) < amount) {
            MessageUtil.sendError(player, "not-enough-money", "amount", String.valueOf(amount));
            return false;
        }

        EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            MessageUtil.sendError(player, "not-enough-money", "amount", String.valueOf(amount));
            return false;
        }

        clan.setBalance(clan.getBalance() + amount);
        clanDatabase.saveClan(clan);
        for (UUID memberId : clan.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                MessageUtil.sendSuccess(member, "invest-broadcast",
                        "player", player.getName(),
                        "amount", String.format("%.0f", amount),
                        "clan", clan.getName());
            }
        }
        updateTopClans();
        return true;
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            MessageUtil.sendError(player, "invalid-amount");
            return false;
        }
        String clanName = playerToClan.get(player.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return false;
        }
        double maxWithdraw = plugin.getConfig().getDouble("settings.max-withdraw", 0);
        if (maxWithdraw > 0 && amount > maxWithdraw) {
            MessageUtil.sendError(player, "withdraw-limit-exceeded", "limit", String.valueOf(maxWithdraw));
            return false;
        }
        if (clan.getBalance() < amount) {
            MessageUtil.sendError(player, "not-enough-clan-money");
            return false;
        }

        EconomyResponse response = plugin.getEconomy().depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            return false;
        }

        clan.setBalance(clan.getBalance() - amount);
        clanDatabase.saveClan(clan);
        for (UUID memberId : clan.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                MessageUtil.sendSuccess(member, "withdraw-broadcast",
                        "player", player.getName(),
                        "amount", String.format("%.0f", amount),
                        "clan", clan.getName());
            }
        }
        updateTopClans();
        return true;
    }

    public void showBalance(Player player) {
        String clanName = playerToClan.get(player.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return;
        }
        MessageUtil.sendSuccess(player, "balance", "balance", String.format("%.0f", clan.getBalance()));
    }

    public void showInfo(Player player) {
        String clanName = playerToClan.get(player.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(player, "clan-not-found", "clan", clanName);
            return;
        }

        String leaderName = plugin.getServer().getOfflinePlayer(clan.getLeader()).getName();
        if (leaderName == null) {
            leaderName = "Неизвестно";
        }

        StringBuilder membersList = new StringBuilder();
        for (UUID memberId : clan.getMembers()) {
            String memberName = Optional.ofNullable(plugin.getServer().getOfflinePlayer(memberId).getName())
                    .orElse("Неизвестно");
            if (membersList.length() > 0) {
                membersList.append(", ");
            }
            membersList.append(memberName);
        }

        MessageUtil.sendList(player, "clan-info",
                "clan", clan.getName(),
                "leader", leaderName,
                "members", membersList.toString(),
                "members_count", String.valueOf(clan.getMembers().size()),
                "kills", String.valueOf(clan.getKills()),
                "balance", String.format("%.0f", clan.getBalance()));
    }

    public void incrementKills(Player killer, Player victim) {
        String killerClanName = playerToClan.get(killer.getUniqueId());
        String victimClanName = playerToClan.get(victim.getUniqueId());
        if (killerClanName != null && !killerClanName.equals(victimClanName)) {
            Clan killerClan = clans.get(killerClanName);
            if (killerClan != null) {
                killerClan.setKills(killerClan.getKills() + 1);
                clanDatabase.saveClan(killerClan);
                updateTopClans();
            }
        }
    }

    public boolean sendClanChat(Player sender, String message) {
        String clanName = playerToClan.get(sender.getUniqueId());
        if (clanName == null) {
            MessageUtil.sendError(sender, "not-in-clan");
            return false;
        }
        Clan clan = clans.get(clanName);
        if (clan == null) {
            MessageUtil.sendError(sender, "clan-not-found", "clan", clanName);
            return false;
        }

        String chatFormat = plugin.getConfig().getString("settings.clan-chat-format", "&6[Клан] &f%player%: &6%message%");
        String formattedMessage = HexUtil.translate(chatFormat
                .replace("%player%", sender.getName())
                .replace("%message%", message));

        for (UUID memberId : clan.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formattedMessage);
            }
        }
        return true;
    }

    public Map<UUID, List<String>> getInvitations() {
        return invitations;
    }

    public Clan getPlayerClan(UUID playerId) {
        return clans.get(playerToClan.get(playerId));
    }

    public ClanGlow getClanGlow() {
        return clanGlow;
    }

    public static class Clan {
        private final String name;
        private final UUID leader;
        private final List<UUID> members;
        private boolean pvpEnabled;
        private double balance;
        private int kills;
        private boolean glowEnabled;
        private Color glowColor;

        public Clan(String name, UUID leader) {
            this(name, leader, new ArrayList<>());
            this.members.add(leader);
        }

        public Clan(String name, UUID leader, List<UUID> members) {
            this.name = name;
            this.leader = leader;
            this.members = new ArrayList<>(members);
            this.pvpEnabled = false;
            this.balance = 0.0;
            this.kills = 0;
            this.glowEnabled = false;
            this.glowColor = Color.fromRGB(0, 255, 0);
        }

        public String getName() {
            return name;
        }

        public UUID getLeader() {
            return leader;
        }

        public List<UUID> getMembers() {
            return members;
        }

        public boolean isPvpEnabled() {
            return pvpEnabled;
        }

        public void setPvpEnabled(boolean pvpEnabled) {
            this.pvpEnabled = pvpEnabled;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public int getKills() {
            return kills;
        }

        public void setKills(int kills) {
            this.kills = kills;
        }

        public boolean isGlowEnabled() {
            return glowEnabled;
        }

        public void setGlowEnabled(boolean glowEnabled) {
            this.glowEnabled = glowEnabled;
        }

        public Color getGlowColor() {
            return glowColor;
        }

        public void setGlowColor(Color glowColor) {
            this.glowColor = glowColor != null ? glowColor : Color.fromRGB(0, 255, 0);
        }
    }
}