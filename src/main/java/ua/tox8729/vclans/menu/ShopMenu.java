package ua.tox8729.vclans.menu;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.PointsManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

public class ShopMenu extends ClanMenu {
    private final PointsManager pointsManager;

    public ShopMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, PointsManager pointsManager, FileConfiguration menuConfig) {
        super(plugin, clanManager, menuManager, menuConfig);
        this.pointsManager = pointsManager;
    }

    @Override
    public void openMenu(Player player) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }

        String title = HexUtil.translate(menuConfig.getString("title", "&0Клановый магазин: %clan_name%")
                .replace("%clan_name%", clan.getName()));
        int size = menuConfig.getInt("size", 27);
        Inventory inventory = Bukkit.createInventory(null, size, title);

        populateInventory(inventory, clan, player);
        checkClanAndOpen(player, inventory);
    }

    public void handleClick(Player player, String itemKey) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.closeInventory();
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }

        String configPath = "items." + itemKey;
        if (!menuConfig.contains(configPath)) {
            plugin.getLogger().warning("Конфигурация для предмета '" + itemKey + "' не найдена в shop-menu.yml!");
            return;
        }

        String currency = menuConfig.getString(configPath + ".currency", "vault").toLowerCase();
        double cost = menuConfig.getDouble(configPath + ".cost", 0.0);
        if (cost <= 0) {
            plugin.getLogger().warning("Недопустимая стоимость (cost <= 0) для предмета '" + itemKey + "' в shop-menu.yml!");
            return;
        }

        if (!currency.equals("points") && !currency.equals("vault")) {
            plugin.getLogger().warning("Недопустимый тип валюты '" + currency + "' для предмета '" + itemKey + "' в shop-menu.yml!");
            return;
        }

        boolean success = false;
        if (currency.equals("points")) {
            success = handlePointsPurchase(player, clan, cost, itemKey);
        } else if (currency.equals("vault")) {
            success = handleVaultPurchase(player, cost, itemKey);
        }

        if (success) {
            executeCommands(player, clan, itemKey);
            MessageUtil.sendSuccess(player, "shop-purchase-success", "item", menuConfig.getString(configPath + ".display-name", itemKey));
        }
    }

    private boolean handlePointsPurchase(Player player, ClanManager.Clan clan, double cost, String itemKey) {
        int playerPoints = pointsManager.getPoints(player.getUniqueId());
        if (playerPoints < cost) {
            MessageUtil.sendError(player, "shop-not-enough-points", "amount", String.format("%.0f", cost));
            return false;
        }
        pointsManager.setPoints(null, player, playerPoints - (int) cost);
        return true;
    }

    private boolean handleVaultPurchase(Player player, double cost, String itemKey) {
        if (plugin.getEconomy().getBalance(player) < cost) {
            MessageUtil.sendError(player, "shop-not-enough-money", "amount", String.format("%.0f", cost));
            return false;
        }
        EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, cost);
        if (!response.transactionSuccess()) {
            MessageUtil.sendError(player, "shop-not-enough-money", "amount", String.format("%.0f", cost));
            return false;
        }
        return true;
    }

    private void executeCommands(Player player, ClanManager.Clan clan, String itemKey) {
        String configPath = "items." + itemKey + ".commands";
        if (!menuConfig.contains(configPath)) return;
        for (String command : menuConfig.getStringList(configPath)) {
            command = command.replace("%player%", player.getName())
                    .replace("%clan_name%", clan.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}