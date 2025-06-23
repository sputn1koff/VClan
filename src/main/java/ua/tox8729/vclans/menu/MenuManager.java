package ua.tox8729.vclans.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.PointsManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

import java.io.File;

public class MenuManager implements Listener {
    private final VClans plugin;
    private final ClanManager clanManager;
    private final PointsManager pointsManager;
    private final MainMenu mainMenu;
    private final PlayerListMenu playerListMenu;
    private final SettingsMenu settingsMenu;
    private final GlowMenu glowMenu;
    private final ShopMenu shopMenu;

    public MenuManager(VClans plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.pointsManager = plugin.getPointsManager();
        loadMenuConfigs();

        this.mainMenu = new MainMenu(plugin, clanManager, this, loadConfig("main-menu.yml"));
        this.playerListMenu = new PlayerListMenu(plugin, clanManager, this, loadConfig("player-list-menu.yml"));
        this.settingsMenu = new SettingsMenu(plugin, clanManager, this, loadConfig("settings-menu.yml"));
        this.glowMenu = new GlowMenu(plugin, clanManager, this, loadConfig("glow-menu.yml"));
        this.shopMenu = new ShopMenu(plugin, clanManager, this, pointsManager, loadConfig("shop-menu.yml"));

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private FileConfiguration loadConfig(String fileName) {
        File menuFolder = new File(plugin.getDataFolder(), "menu");
        File configFile = new File(menuFolder, fileName);
        return YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadMenuConfigs() {
        File menuFolder = new File(plugin.getDataFolder(), "menu");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
            plugin.saveResource("menu/main-menu.yml", false);
            plugin.saveResource("menu/player-list-menu.yml", false);
            plugin.saveResource("menu/settings-menu.yml", false);
            plugin.saveResource("menu/glow-menu.yml", false);
            plugin.saveResource("menu/shop-menu.yml", false);
        }
    }

    public void reloadMenus() {
        loadMenuConfigs();
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public PlayerListMenu getPlayerListMenu() {
        return playerListMenu;
    }

    public SettingsMenu getSettingsMenu() {
        return settingsMenu;
    }

    public GlowMenu getGlowMenu() {
        return glowMenu;
    }

    public ShopMenu getShopMenu() {
        return shopMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        ClanMenu targetMenu = identifyMenu(title);
        if (targetMenu == null) return;

        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.closeInventory();
            MessageUtil.sendError(player, "not-in-clan");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == org.bukkit.Material.AIR || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        int customModelData = meta.hasCustomModelData() ? meta.getCustomModelData() : -1;
        FileConfiguration menuConfig = targetMenu.menuConfig;

        for (String key : menuConfig.getConfigurationSection("items").getKeys(false)) {
            if (key.equals("player-head") || key.equals("decor")) continue;
            if (customModelData == key.hashCode()) {
                if (menuConfig.getBoolean("items." + key + ".leader-only", false) && !player.getUniqueId().equals(clan.getLeader())) {
                    player.sendMessage(HexUtil.translate(menuConfig.getString("messages.leader-only", "&cЭта команда доступна только лидеру клана!")));
                    return;
                }
                playSound(player, menuConfig.getString("items." + key + ".sound"));
                if ("back".equals(key)) {
                    mainMenu.openMenu(player);
                    return;
                }
                handleMenuSpecificClick(targetMenu, player, key, event.getClick() == ClickType.RIGHT);
            }
        }
    }

    private ClanMenu identifyMenu(String title) {
        String mainMenuTitle = HexUtil.translate(mainMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String playerListTitle = HexUtil.translate(playerListMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String settingsTitle = HexUtil.translate(settingsMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String glowTitle = HexUtil.translate(glowMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String shopTitle = HexUtil.translate(shopMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        if (title.contains(mainMenuTitle)) return mainMenu;
        if (title.contains(playerListTitle)) return playerListMenu;
        if (title.contains(settingsTitle)) return settingsMenu;
        if (title.contains(glowTitle)) return glowMenu;
        if (title.contains(shopTitle)) return shopMenu;
        return null;
    }

    private void playSound(Player player, String soundName) {
        if (soundName != null && !soundName.isEmpty()) {
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректное название звука в конфиге: " + soundName);
            }
        }
    }

    private void handleMenuSpecificClick(ClanMenu targetMenu, Player player, String key, boolean isRightClick) {
        if (targetMenu instanceof SettingsMenu) {
            ((SettingsMenu) targetMenu).handleClick(player, key, isRightClick);
        } else if (targetMenu instanceof MainMenu) {
            ((MainMenu) targetMenu).handleClick(player, key);
        } else if (targetMenu instanceof PlayerListMenu) {
            ((PlayerListMenu) targetMenu).handleClick(player, key);
        } else if (targetMenu instanceof GlowMenu) {
            ((GlowMenu) targetMenu).handleClick(player, key);
        } else if (targetMenu instanceof ShopMenu) {
            ((ShopMenu) targetMenu).handleClick(player, key);
        }
    }
}