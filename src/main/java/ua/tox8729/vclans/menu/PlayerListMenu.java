package ua.tox8729.vclans.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.HexUtil;
import ua.tox8729.vclans.utils.MessageUtil;

import java.util.*;

public class PlayerListMenu extends ClanMenu {
    private final MenuManager menuManager;
    private final List<Integer> playerSlots;

    public PlayerListMenu(VClans plugin, ClanManager clanManager, MenuManager menuManager, FileConfiguration menuConfig) {
        super(plugin, clanManager, menuConfig);
        this.menuManager = menuManager;
        this.playerSlots = initializePlayerSlots(menuConfig);
    }

    private List<Integer> initializePlayerSlots(FileConfiguration config) {
        List<Integer> slots = config.getIntegerList("player-slots");
        if (slots.isEmpty()) {
            int size = config.getInt("size", 54);
            slots = new ArrayList<>(size - 9);
            for (int i = 0; i < size - 9; i++) {
                slots.add(i);
            }
        }
        return Collections.unmodifiableList(slots);
    }

    @Override
    public void openMenu(Player player) {
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "not-in-clan");
            return;
        }

        String title = HexUtil.translate(menuConfig.getString("title", "&0Участники клана: %clan_name%")
                .replace("%clan_name%", clan.getName()));
        int size = Math.max(9, Math.min(54, menuConfig.getInt("size", 54)));
        Inventory inventory = Bukkit.createInventory(null, size, title);

        populatePlayerHeads(inventory, clan, size);
        populateInventory(inventory, clan, player);
        checkClanAndOpen(player, inventory);
    }

    private void populatePlayerHeads(Inventory inventory, ClanManager.Clan clan, int size) {
        String leaderRoleText = HexUtil.translate(menuConfig.getString("leader-role-text", "&#F0DB46Лидер"));
        String memberRoleText = HexUtil.translate(menuConfig.getString("member-role-text", "&7Учасник"));
        String onlineText = HexUtil.translate(menuConfig.getString("online-text", "D13E0Онлайн"));
        String offlineText = HexUtil.translate(menuConfig.getString("offline-text", "&#EF3434Оффлайн"));
        String headDisplayName = HexUtil.translate(menuConfig.getString("items.player-head.display-name", "&6%player_name%"));
        List<String> headLoreTemplate = menuConfig.getStringList("items.player-head.lore");
        boolean hideAttributes = menuConfig.getBoolean("items.player-head.hide-attributes", false);

        int slotIndex = 0;
        for (UUID memberId : clan.getMembers()) {
            if (slotIndex >= playerSlots.size()) {
                break;
            }
            int slot = playerSlots.get(slotIndex++);
            if (slot < 0 || slot >= size) {
                continue;
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) {
                continue;
            }

            if (hideAttributes) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            }

            OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(memberId);
            String playerName = Optional.ofNullable(offlinePlayer.getName()).orElse("Неизвестно");
            meta.setOwningPlayer(offlinePlayer);

            String role = memberId.equals(clan.getLeader()) ? leaderRoleText : memberRoleText;
            String onlineStatus = Bukkit.getPlayer(memberId) != null ? onlineText : offlineText;

            meta.setDisplayName(headDisplayName.replace("%player_name%", playerName));

            List<String> translatedLore = new ArrayList<>(headLoreTemplate.size());
            for (String line : headLoreTemplate) {
                translatedLore.add(HexUtil.translate(line)
                        .replace("%player_name%", playerName)
                        .replace("%player_role%", role)
                        .replace("%player_online%", onlineStatus));
            }
            meta.setLore(translatedLore);

            meta.setCustomModelData(playerName.hashCode());
            head.setItemMeta(meta);
            inventory.setItem(slot, head);
        }
    }

    public void handleClick(Player player, String itemKey) {
        if ("back".equals(itemKey)) {
            menuManager.getMainMenu().openMenu(player);
        }
    }
}