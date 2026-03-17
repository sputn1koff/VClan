package ua.tox8729.vclans.menu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

import java.util.*;

public class SettingsMenu extends ClanMenu {
    private final Map<UUID, Integer> selectedAmountIndex;
    private final List<Double> presetAmounts;

    public SettingsMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, FileConfiguration menuConfig) {
        super(plugin, clanManager, menuManager, menuConfig);
        this.selectedAmountIndex = new HashMap<>();
        this.presetAmounts = new ArrayList<>();

        List<Double> amounts = menuConfig.getDoubleList("presets.amounts");
        if (amounts.isEmpty()) {
            amounts = Arrays.asList(100.0, 1000.0, 10000.0);
        }
        presetAmounts.addAll(amounts);
    }

    @Override
    public void openMenu(Player player) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }

        String title = HexUtil.translate(menuConfig.getString("title", "&0Клан: %clan_name%")
                .replace("%clan_name%", clan.getName()));
        int size = menuConfig.getInt("size", 27);
        Inventory inventory = Bukkit.createInventory(null, size, title);

        selectedAmountIndex.putIfAbsent(player.getUniqueId(), 0);
        populateInventory(inventory, clan, player);
        checkClanAndOpen(player, inventory);
    }

    public void handleClick(Player player, String itemKey, boolean isRightClick) {
        int currentIndex = selectedAmountIndex.getOrDefault(player.getUniqueId(), 0);
        switch (itemKey) {
            case "toggle-pvp":
                clanManager.togglePvp(player);
                openMenu(player);
                break;
            case "deposit":
                if (isRightClick) {
                    selectedAmountIndex.put(player.getUniqueId(), (currentIndex + 1) % presetAmounts.size());
                    openMenu(player);
                } else {
                    double amount = presetAmounts.get(currentIndex);
                    clanManager.invest(player, amount);
                }
                break;
            case "withdraw":
                if (isRightClick) {
                    selectedAmountIndex.put(player.getUniqueId(), (currentIndex + 1) % presetAmounts.size());
                    openMenu(player);
                } else {
                    double amount = presetAmounts.get(currentIndex);
                    clanManager.withdraw(player, amount);
                }
                break;
            case "back":
                menuManager.getMainMenu().openMenu(player);
                break;
        }
    }

    @Override
    protected ItemStack createItem(ClanManager.Clan clan, Player player, String configPath, String itemKey) {
        ItemStack item = super.createItem(clan, player, configPath, itemKey);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        int index = selectedAmountIndex.getOrDefault(player.getUniqueId(), 0);
        String selectedAmount = String.format("%.0f", presetAmounts.get(Math.min(index, presetAmounts.size() - 1)));
        String playerBalance = String.format("%.0f", plugin.getEconomy().getBalance(player));

        String displayName = meta.getDisplayName()
                .replace("%selected_amount%", selectedAmount)
                .replace("%player_balance%", playerBalance);

        List<String> lore = meta.getLore();
        if (lore != null) {
            List<String> updatedLore = new ArrayList<>();
            for (String line : lore) {
                updatedLore.add(line
                        .replace("%selected_amount%", selectedAmount)
                        .replace("%player_balance%", playerBalance));
            }
            meta.setLore(updatedLore);
        }

        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }
}