package ua.tox8729.vclans.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.PointsManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MenuManager implements Listener {
    private final VClans plugin;
    private final ClanManager clanManager;
    private final PointsManager pointsManager;
    private final MainMenu mainMenu;
    private final PlayerListMenu playerListMenu;
    private final SettingsMenu settingsMenu;
    private final GlowMenu glowMenu;
    private final ShopMenu shopMenu;

    // -------------------------------------------------------
    // Animation state
    // -------------------------------------------------------
    private static final Map<UUID, BukkitTask> activeAnimations = new HashMap<>();

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

    // -------------------------------------------------------
    // Animation helpers
    // -------------------------------------------------------

    /**
     * Cancels any running animation for the given player.
     */
    public void cancelAnimation(UUID uuid) {
        BukkitTask task = activeAnimations.remove(uuid);
        if (task != null) task.cancel();
    }

    private List<List<Integer>> buildFrames(String type, Set<Integer> slots, int rows) {
        List<List<Integer>> frames = new ArrayList<>();
        switch (type) {
            case "RIGHT":
                for (int col = 0; col < 9; col++) {
                    List<Integer> f = new ArrayList<>();
                    for (int s : slots) if (s % 9 == col) f.add(s);
                    if (!f.isEmpty()) frames.add(f);
                }
                break;
            case "LEFT":
                for (int col = 8; col >= 0; col--) {
                    List<Integer> f = new ArrayList<>();
                    for (int s : slots) if (s % 9 == col) f.add(s);
                    if (!f.isEmpty()) frames.add(f);
                }
                break;
            case "TOP_DOWN":
                for (int row = 0; row < rows; row++) {
                    List<Integer> f = new ArrayList<>();
                    for (int s : slots) if (s / 9 == row) f.add(s);
                    if (!f.isEmpty()) frames.add(f);
                }
                break;
            case "BOTTOM_UP":
                for (int row = rows - 1; row >= 0; row--) {
                    List<Integer> f = new ArrayList<>();
                    for (int s : slots) if (s / 9 == row) f.add(s);
                    if (!f.isEmpty()) frames.add(f);
                }
                break;
            case "CENTER":
            default:
                int l = 0, r = 8;
                while (l <= r) {
                    List<Integer> f = new ArrayList<>();
                    for (int s : slots) { int c = s % 9; if (c == l || c == r) f.add(s); }
                    if (!f.isEmpty()) frames.add(f);
                    l++; r--;
                }
                break;
        }
        return frames;
    }

    /**
     * Opens an inventory with animation.
     * Call this from each menu's openMenu() instead of player.openInventory(inv).
     *
     * Usage in a menu class:
     *   Map<Integer, ItemStack> slots = new LinkedHashMap<>();
     *   for (int s = 0; s < inv.getSize(); s++) {
     *       ItemStack it = inv.getItem(s);
     *       if (it != null) { slots.put(s, it.clone()); inv.setItem(s, null); }
     *   }
     *   menuManager.openWithAnimation(player, inv, slots, menuConfig);
     *
     * The animation settings are read from the menu's own config under "animation:".
     */
    public void openWithAnimation(Player player, Inventory inv,
                                  Map<Integer, ItemStack> targetSlots,
                                  FileConfiguration menuConfig) {
        cancelAnimation(player.getUniqueId());

        boolean enabled = menuConfig.getBoolean("animation.enabled", false);

        player.openInventory(inv);

        if (!enabled || targetSlots.isEmpty()) {
            targetSlots.forEach(inv::setItem);
            return;
        }

        String type = menuConfig.getString("animation.type", "CENTER").toUpperCase();
        if (type.equals("RANDOM")) {
            String[] types = {"CENTER", "RIGHT", "LEFT", "TOP_DOWN", "BOTTOM_UP"};
            type = types[new Random().nextInt(types.length)];
        }

        int delay = menuConfig.getInt("animation.delay-ticks", 2);
        int rows   = inv.getSize() / 9;

        // Filler during animation
        String fillerMatName = menuConfig.getString("animation.filler-material", "AIR");
        ItemStack filler = null;
        try {
            Material fillerMat = Material.matchMaterial(fillerMatName.toUpperCase());
            if (fillerMat != null && fillerMat != Material.AIR) {
                filler = new ItemStack(fillerMat);
                ItemMeta fillerMeta = filler.getItemMeta();
                if (fillerMeta != null) {
                    String fillerName = menuConfig.getString("animation.filler-name", "");
                    fillerMeta.setDisplayName(HexUtil.translate(fillerName));
                    filler.setItemMeta(fillerMeta);
                }
            }
        } catch (Exception ignored) {}

        final ItemStack fillerItem = filler;
        for (int slot : targetSlots.keySet()) {
            inv.setItem(slot, fillerItem);
        }

        List<List<Integer>> frames = buildFrames(type, targetSlots.keySet(), rows);
        final int[] frameIdx = {0};

        boolean soundEnabled = menuConfig.getBoolean("animation.sound.enabled", false);
        String  soundName    = menuConfig.getString("animation.sound.name", "UI_BUTTON_CLICK");
        float   soundVolume  = (float) menuConfig.getDouble("animation.sound.volume", 0.5);
        float   soundPitch   = (float) menuConfig.getDouble("animation.sound.pitch", 1.5);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (frameIdx[0] >= frames.size() || !player.isOnline()) {
                targetSlots.forEach(inv::setItem);
                cancelAnimation(player.getUniqueId());
                return;
            }
            for (int slot : frames.get(frameIdx[0])) {
                ItemStack item = targetSlots.get(slot);
                if (item != null) inv.setItem(slot, item);
            }
            if (soundEnabled) {
                try {
                    player.playSound(player.getLocation(),
                            Sound.valueOf(soundName.toUpperCase()),
                            soundVolume, soundPitch);
                } catch (Exception ignored) {}
            }
            frameIdx[0]++;
        }, delay, delay);

        activeAnimations.put(player.getUniqueId(), task);
    }

    /**
     * Convenience helper: collects all non-null items from the inventory,
     * clears them, then calls openWithAnimation.
     *
     * Use this if you don't need to separate "static" vs "animated" slots —
     * just build the inventory normally and pass it here.
     */
    public void openWithAnimation(Player player, Inventory inv, FileConfiguration menuConfig) {
        cancelAnimation(player.getUniqueId());
        Map<Integer, ItemStack> slots = new LinkedHashMap<>();
        for (int s = 0; s < inv.getSize(); s++) {
            ItemStack it = inv.getItem(s);
            if (it != null) {
                slots.put(s, it.clone());
                inv.setItem(s, null);
            }
        }
        openWithAnimation(player, inv, slots, menuConfig);
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

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

    // -------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        cancelAnimation(player.getUniqueId());
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
        String mainMenuTitle   = HexUtil.translate(mainMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String playerListTitle = HexUtil.translate(playerListMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String settingsTitle   = HexUtil.translate(settingsMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String glowTitle       = HexUtil.translate(glowMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        String shopTitle       = HexUtil.translate(shopMenu.menuConfig.getString("title", "").replace("%clan_name%", ""));
        if (title.contains(mainMenuTitle))   return mainMenu;
        if (title.contains(playerListTitle)) return playerListMenu;
        if (title.contains(settingsTitle))   return settingsMenu;
        if (title.contains(glowTitle))       return glowMenu;
        if (title.contains(shopTitle))       return shopMenu;
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