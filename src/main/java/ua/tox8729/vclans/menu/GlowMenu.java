package ua.tox8729.vclans.menu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

public class GlowMenu extends ClanMenu {

    public GlowMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, FileConfiguration menuConfig) {
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
            case "toggle-glow":
                clanManager.toggleGlow(player);
                openMenu(player);
                break;
            case "color-black":
            case "color-white":
            case "color-fuchsia":
            case "color-red":
            case "color-yellow":
            case "color-lime":
            case "color-green":
            case "color-aqua":
            case "color-blue":
            case "color-orange":
                String colorString = menuConfig.getString("items." + itemKey + ".color", "0,255,0");
                clanManager.setGlowColor(player, colorString);
                openMenu(player);
                break;
            case "back":
                menuManager.getMainMenu().openMenu(player);
                break;
        }
    }
}