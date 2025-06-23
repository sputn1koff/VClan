package ua.tox8729.vclans;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;
import ua.tox8729.vclans.commands.ClanCommand;
import ua.tox8729.vclans.commands.VClansCommand;
import ua.tox8729.vclans.commands.ClanCommandTabCompleter;
import ua.tox8729.vclans.commands.VClansCommandTabCompleter;
import ua.tox8729.vclans.database.DatabaseManager;
import ua.tox8729.vclans.database.ClanDatabase;
import ua.tox8729.vclans.listeners.GlowListener;
import ua.tox8729.vclans.listeners.PlayerListener;
import ua.tox8729.vclans.managers.ClanManager;
import ua.tox8729.vclans.managers.PointsManager;
import ua.tox8729.vclans.menu.MenuManager;
import ua.tox8729.vclans.utils.MessageUtil;
import ua.tox8729.vclans.utils.VClansPlaceholderExpansion;
import com.comphenix.protocol.ProtocolLibrary;

public class VClans extends JavaPlugin {
    private DatabaseManager databaseManager;
    private ClanManager clanManager;
    private PointsManager pointsManager;
    private Economy economy;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        MessageUtil.init(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null ||
                getServer().getPluginManager().getPlugin("ProtocolLib") == null ||
                !setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        ClanDatabase clanDatabase = new ClanDatabase(databaseManager);
        clanManager = new ClanManager(clanDatabase, this);
        pointsManager = new PointsManager(clanDatabase);
        menuManager = new MenuManager(this, clanManager);

        getCommand("clan").setExecutor(new ClanCommand(clanManager, this));
        getCommand("clan").setTabCompleter(new ClanCommandTabCompleter(clanManager));
        getCommand("vclans").setExecutor(new VClansCommand(this));
        getCommand("vclans").setTabCompleter(new VClansCommandTabCompleter());

        getServer().getPluginManager().registerEvents(new PlayerListener(clanManager, clanManager.getClanGlow()), this);
        ProtocolLibrary.getProtocolManager().addPacketListener(new GlowListener(this, clanManager, clanManager.getClanGlow()));

        new VClansPlaceholderExpansion(clanManager, pointsManager).register();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public PointsManager getPointsManager() {
        return pointsManager;
    }

    public Economy getEconomy() {
        if (economy == null) {
            throw new IllegalStateException("Экономика не инициализирована! Убедитесь что Vault и экономический плагин установлены!");
        }
        return economy;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void reloadPlugin() {
        reloadConfig();
        MessageUtil.init(this);
        if (menuManager != null) {
            HandlerList.unregisterAll(menuManager);
        }
        menuManager = new MenuManager(this, clanManager);
        menuManager.reloadMenus();
        clanManager.reloadBlockedNames();
    }
}