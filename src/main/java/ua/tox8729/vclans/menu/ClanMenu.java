package ua.tox8729.vclans.menu;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;

public abstract class ClanMenu {
    protected final VClans plugin;
    protected final ClanManager clanManager;
    protected final MenuManager menuManager; // <-- добавлено
    protected FileConfiguration menuConfig;

    // -------------------------------------------------------
    // Base64 skull helpers (reflection, no authlib import)
    // -------------------------------------------------------

    private static boolean isBase64Texture(String value) {
        if (value == null || value.length() < 20) return false;
        try {
            String decoded = new String(Base64.getDecoder().decode(value));
            return decoded.contains("textures");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack buildSkullFromBase64(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        try {
            Class<?> gpClass   = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile  = gpClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.nameUUIDFromBytes(base64.getBytes()), "CustomSkull");
            Object property = propClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);
            Object propMap  = gpClass.getMethod("getProperties").invoke(profile);
            propMap.getClass().getMethod("put", Object.class, Object.class).invoke(propMap, "textures", property);
            Field pf = meta.getClass().getDeclaredField("profile");
            pf.setAccessible(true);
            pf.set(meta, profile);
        } catch (Exception ex) {
            plugin.getLogger().warning("[VClans] Could not apply skull texture: " + ex.getMessage());
        }
        skull.setItemMeta(meta);
        return skull;
    }

    public ClanMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, FileConfiguration menuConfig) {
        this.plugin = plugin;
        this.clanManager = clanManager;
        this.menuManager = menuManager; // <-- добавлено
        this.menuConfig = menuConfig;
    }

    public abstract void openMenu(Player player);

    protected ItemStack createItem(ClanManager.Clan clan, Player player, String configPath, String itemKey) {
        String materialName = menuConfig.getString(configPath + ".material", "PAPER");

        ItemStack item;
        Material material;
        if (isBase64Texture(materialName)) {
            item     = buildSkullFromBase64(materialName);
            material = Material.PLAYER_HEAD;
        } else {
            material = Material.getMaterial(materialName);
            if (material == null) material = Material.BOOK;
            item = new ItemStack(material);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        boolean hideAttributes = menuConfig.getBoolean(configPath + ".hide-attributes", false);
        if (hideAttributes) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
            }
        }

        if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
            String colorString = menuConfig.getString(configPath + ".potion-color");
            if (colorString != null && !colorString.isEmpty()) {
                try {
                    String[] rgb = colorString.split(",");
                    if (rgb.length == 3) {
                        int r = Integer.parseInt(rgb[0].trim());
                        int g = Integer.parseInt(rgb[1].trim());
                        int b = Integer.parseInt(rgb[2].trim());
                        org.bukkit.inventory.meta.PotionMeta potionMeta = (org.bukkit.inventory.meta.PotionMeta) meta;
                        potionMeta.setColor(org.bukkit.Color.fromRGB(r, g, b));
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    plugin.getLogger().warning("Некорректный формат potion-color для предмета '" + itemKey + "': " + colorString);
                }
            }
        }

        String leaderName = plugin.getServer().getOfflinePlayer(clan.getLeader()).getName();
        if (leaderName == null) leaderName = "Неизвестно";

        String pvpStatus = HexUtil.translate(clan.isPvpEnabled()
                ? menuConfig.getString("pvp-enabled-text", "C500Включено")
                : menuConfig.getString("pvp-disabled-text", "&#C50000Выключено"));
        String pvpToggleAction = clan.isPvpEnabled() ? "выключить" : "включить";

        String glowStatus = HexUtil.translate(clan.isGlowEnabled()
                ? menuConfig.getString("glow-enabled-text", "C500Включено")
                : menuConfig.getString("glow-disabled-text", "&#C50000Выключено"));
        String glowToggleAction = clan.isGlowEnabled() ? "выключить" : "включить";

        double cost = menuConfig.getDouble(configPath + ".cost", 0.0);

        String displayName = HexUtil.translate(menuConfig.getString(configPath + ".display-name", ""))
                .replace("%clan_name%", clan.getName())
                .replace("%leader_name%", leaderName)
                .replace("%members_count%", String.valueOf(clan.getMembers().size()))
                .replace("%balance%", String.format("%.0f", clan.getBalance()))
                .replace("%kills%", String.valueOf(clan.getKills()))
                .replace("%pvp_status%", pvpStatus)
                .replace("%pvp_toggle_action%", pvpToggleAction)
                .replace("%glow_status%", glowStatus)
                .replace("%glow_toggle_action%", glowToggleAction)
                .replace("%cost%", String.format("%.0f", cost));

        meta.setDisplayName(displayName);

        List<String> lore = menuConfig.getStringList(configPath + ".lore");
        List<String> translatedLore = new ArrayList<>();
        for (String line : lore) {
            translatedLore.add(HexUtil.translate(line)
                    .replace("%clan_name%", clan.getName())
                    .replace("%leader_name%", leaderName)
                    .replace("%members_count%", String.valueOf(clan.getMembers().size()))
                    .replace("%balance%", String.format("%.0f", clan.getBalance()))
                    .replace("%kills%", String.valueOf(clan.getKills()))
                    .replace("%pvp_status%", pvpStatus)
                    .replace("%pvp_toggle_action%", pvpToggleAction)
                    .replace("%glow_status%", glowStatus)
                    .replace("%glow_toggle_action%", glowToggleAction)
                    .replace("%cost%", String.format("%.0f", cost)));
        }
        meta.setLore(translatedLore);

        if (!itemKey.equals("player-head")) {
            meta.setCustomModelData(itemKey.hashCode());
        }
        item.setItemMeta(meta);
        return item;
    }

    protected void populateInventory(Inventory inventory, ClanManager.Clan clan, Player player) {
        int size = menuConfig.getInt("size", 27);
        for (String key : menuConfig.getConfigurationSection("items").getKeys(false)) {
            String configPath = "items." + key;
            ItemStack item = createItem(clan, player, configPath, key);
            if (menuConfig.contains(configPath + ".slots")) {
                List<Integer> slots = menuConfig.getIntegerList(configPath + ".slots");
                for (int slot : slots) {
                    if (slot >= 0 && slot < size) {
                        inventory.setItem(slot, item);
                    }
                }
            } else {
                int slot = menuConfig.getInt(configPath + ".slot", -1);
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    protected void checkClanAndOpen(Player player, Inventory inventory) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }
        // Было: player.openInventory(inventory);
        menuManager.openWithAnimation(player, inventory, menuConfig);
    }
}