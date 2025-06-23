package ua.tox8729.vclans.commands.subcommands;

import org.bukkit.entity.Player;
import ua.tox8729.vclans.VClans;
import ua.tox8729.vclans.managers.ClanManager;

public class GlowCommand {
    private final ClanManager clanManager;
    private final VClans plugin;

    public GlowCommand(ClanManager clanManager, VClans plugin) {
        this.clanManager = clanManager;
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        return clanManager.toggleGlow(player);
    }
}