package ua.tox8729.vclans.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.utils.ClanGlow;

public class PlayerListener implements Listener {
    private final ClanManager clanManager;
    private final ClanGlow clanGlow;

    public PlayerListener(ClanManager clanManager, ClanGlow clanGlow) {
        this.clanManager = clanManager;
        this.clanGlow = clanGlow;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        ClanManager.Clan damagerClan = clanManager.getPlayerClan(damager.getUniqueId());
        ClanManager.Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());

        if (damagerClan != null && damagerClan.equals(victimClan) && !damagerClan.isPvpEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {
            clanManager.incrementKills(killer, victim);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (clanManager.getPlayerClan(player.getUniqueId()) != null) {
            clanGlow.changeForPlayer(player, clanManager);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ClanManager.Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan != null) {
            clanGlow.changeForPlayer(player, clanManager);
            clanGlow.updateAllMembersForPlayer(player, clanManager);
        }
    }
}