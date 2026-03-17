package ua.tox8729.vclans.menu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

public class MainMenu extends ClanMenu {

    public MainMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, FileConfiguration menuConfig) {
        super(plugin, clanManager, menuManager, menuConfig);
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

        populateInventory(inventory, clan, player);
        checkClanAndOpen(player, inventory);
    }

    public void handleClick(Player player, String itemKey) {
        switch (itemKey) {
            case "show-info":
                clanManager.showInfo(player);
                break;
            case "open-player-list":
                menuManager.getPlayerListMenu().openMenu(player);
                break;
            case "open-settings":
                menuManager.getSettingsMenu().openMenu(player);
                break;
            case "open-glow-menu":
                menuManager.getGlowMenu().openMenu(player);
                break;
            case "open-shop":
                menuManager.getShopMenu().openMenu(player);
                break;
        }
    }
}