package ua.tox8729.vclans.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import ua.tox8729.vclans.database.ClanDatabase;
import ua.tox8729.vclans.managers.ClanManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanGlow {
    private final ClanDatabase clanDatabase;

    public ClanGlow(ClanDatabase clanDatabase) {
        this.clanDatabase = clanDatabase;
    }

    public void changeForPlayer(Player receiver, ClanManager clanManager) {
        ClanManager.Clan receiverClan = clanManager.getPlayerClan(receiver.getUniqueId());
        if (receiverClan == null) {
            resetGlowForPlayer(receiver);
            return;
        }

        for (UUID memberId : receiverClan.getMembers()) {
            Player sender = receiver.getServer().getPlayer(memberId);
            if (sender == null || sender == receiver) continue;

            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, sender.getEntityId());

            PlayerInventory inventory = sender.getInventory();
            List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>();
            if (receiverClan.isGlowEnabled()) {
                list.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, inventory.getHelmet()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, inventory.getChestplate()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, inventory.getLeggings()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.FEET, inventory.getBoots()));
            } else {
                list.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, inventory.getHelmet()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, inventory.getChestplate()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, inventory.getLeggings()));
                list.add(new Pair<>(EnumWrappers.ItemSlot.FEET, inventory.getBoots()));
            }
            list.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, inventory.getItemInMainHand()));
            list.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, inventory.getItemInOffHand()));
            packet.getSlotStackPairLists().write(0, list);

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateAllMembersForPlayer(Player receiver, ClanManager clanManager) {
        ClanManager.Clan receiverClan = clanManager.getPlayerClan(receiver.getUniqueId());
        if (receiverClan == null) {
            resetGlowForPlayer(receiver);
            return;
        }

        for (UUID memberId : receiverClan.getMembers()) {
            if (memberId.equals(receiver.getUniqueId())) continue;
            Player member = receiver.getServer().getPlayer(memberId);
            if (member != null) {
                this.changeForPlayer(member, clanManager);
            }
        }
    }

    public void resetGlowForPlayer(Player receiver) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, receiver.getEntityId());

        PlayerInventory inventory = receiver.getInventory();
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = new ArrayList<>();
        list.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, inventory.getHelmet()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, inventory.getChestplate()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, inventory.getLeggings()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.FEET, inventory.getBoots()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, inventory.getItemInMainHand()));
        list.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, inventory.getItemInOffHand()));
        packet.getSlotStackPairLists().write(0, list);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}