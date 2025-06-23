package ua.tox8729.vclans.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.ClanGlow;

import java.util.ArrayList;
import java.util.List;

public class GlowListener extends PacketAdapter {
    private final ClanManager clanManager;
    private final Plugin plugin;
    private final ClanGlow clanGlow;

    public GlowListener(Plugin plugin, ClanManager clanManager, ClanGlow clanGlow) {
        super(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.clanManager = clanManager;
        this.plugin = plugin;
        this.clanGlow = clanGlow;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player receiver = event.getPlayer();
        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        Player sender = null;
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getEntityId() == entityId) {
                sender = onlinePlayer;
                break;
            }
        }

        if (sender == null || sender == receiver) return;

        ClanManager.Clan senderClan = clanManager.getPlayerClan(sender.getUniqueId());
        ClanManager.Clan receiverClan = clanManager.getPlayerClan(receiver.getUniqueId());

        if (senderClan == null || receiverClan == null || !senderClan.getName().equals(receiverClan.getName()) || !receiverClan.isGlowEnabled()) {
            setDefaultEquipment(packet, sender);
            return;
        }

        Color color = senderClan.getGlowColor();
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>();
        PlayerInventory inventory = sender.getInventory();

        list.add(createColoredArmor(EnumWrappers.ItemSlot.HEAD, inventory.getHelmet(), Material.LEATHER_HELMET, color));
        list.add(createColoredArmor(EnumWrappers.ItemSlot.CHEST, inventory.getChestplate(), Material.LEATHER_CHESTPLATE, color));
        list.add(createColoredArmor(EnumWrappers.ItemSlot.LEGS, inventory.getLeggings(), Material.LEATHER_LEGGINGS, color));
        list.add(createColoredArmor(EnumWrappers.ItemSlot.FEET, inventory.getBoots(), Material.LEATHER_BOOTS, color));
        list.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, inventory.getItemInMainHand()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, inventory.getItemInOffHand()));

        packet.getSlotStackPairLists().write(0, list);
    }

    private void setDefaultEquipment(PacketContainer packet, Player sender) {
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>();
        PlayerInventory inventory = sender.getInventory();
        list.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, inventory.getHelmet()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, inventory.getChestplate()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, inventory.getLeggings()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.FEET, inventory.getBoots()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, inventory.getItemInMainHand()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, inventory.getItemInOffHand()));
        packet.getSlotStackPairLists().write(0, list);
    }

    private Pair<EnumWrappers.ItemSlot, ItemStack> createColoredArmor(EnumWrappers.ItemSlot slot, ItemStack current, Material leatherType, Color color) {
        ItemStack leather = new ItemStack(leatherType);
        LeatherArmorMeta meta = (LeatherArmorMeta) leather.getItemMeta();
        meta.setColor(color);
        leather.setItemMeta(meta);
        return new Pair<>(slot, leather);
    }
}