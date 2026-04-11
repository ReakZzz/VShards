package dev.reakkz.vshards;

import dev.reakkz.vshards.commands.ShardsCommand;
import dev.reakkz.vshards.listeners.ShopListener;
import dev.reakkz.vshards.managers.DataManager;
import dev.reakkz.vshards.managers.ShardsManager;
import dev.reakkz.vshards.placeholders.ShardsPlaceholder;
import dev.reakkz.vshards.shop.ShopGUI;
import dev.reakkz.vshards.shop.ShopManager;
import dev.reakkz.vshards.tasks.AutoSaveTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VShards extends JavaPlugin {

    private static VShards instance;

    private ShardsManager shardsManager;
    private DataManager dataManager;
    private ShopManager shopManager;
    private ShopGUI shopGUI;
    private AutoSaveTask autoSaveTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Core managers
        this.dataManager   = new DataManager(this);
        this.shardsManager = new ShardsManager(this, dataManager);
        dataManager.loadAll();

        // Shop
        this.shopManager = new ShopManager(this);
        this.shopManager.load();
        this.shopGUI = new ShopGUI(this);

        // Commands
        ShardsCommand executor = new ShardsCommand(this);
        PluginCommand cmd = getCommand("shards");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShardsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI found — placeholders registered.");
        } else {
            getLogger().info("PlaceholderAPI not found — placeholders unavailable.");
        }

        // Auto-save
        startAutoSave();

        getLogger().info("V-Shards v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (dataManager != null) dataManager.saveAll();
        getLogger().info("V-Shards disabled. All data saved.");
    }

    public void reload() {
        reloadConfig();
        shopManager.load();
        if (autoSaveTask != null) autoSaveTask.cancel();
        startAutoSave();
    }

    private void startAutoSave() {
        int seconds = getConfig().getInt("storage.auto-save-interval", 300);
        long ticks  = seconds * 20L;
        autoSaveTask = new AutoSaveTask(this);
        autoSaveTask.runTaskTimerAsynchronously(this, ticks, ticks);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public static VShards getInstance()      { return instance; }
    public ShardsManager getShardsManager()  { return shardsManager; }
    public DataManager getDataManager()      { return dataManager; }
    public ShopManager getShopManager()      { return shopManager; }
    public ShopGUI getShopGUI()              { return shopGUI; }
}
