package dev.reakkz.vshards.managers;

import dev.reakkz.vshards.VShards;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles reading and writing of player balance data to a YAML flat-file.
 * Only "dirty" entries (modified since the last save) are written back,
 * keeping IO minimal.
 */
public class DataManager {

    private final VShards plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    /** UUIDs that have unsaved changes. */
    private final Set<UUID> dirtyEntries = ConcurrentHashMap.newKeySet();

    public DataManager(VShards plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    /** Reads all stored balances from data.yml into the ShardsManager map. */
    public void loadAll() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data.yml!", e);
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        Map<UUID, Long> balances = plugin.getShardsManager().getBalanceMap();

        if (!dataConfig.isConfigurationSection("balances")) return;

        Objects.requireNonNull(dataConfig.getConfigurationSection("balances"))
                .getKeys(false)
                .forEach(key -> {
                    try {
                        UUID uuid = UUID.fromString(key);
                        long amount = dataConfig.getLong("balances." + key, 0);
                        balances.put(uuid, amount);
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Skipping malformed UUID in data.yml: " + key);
                    }
                });

        plugin.getLogger().info("Loaded " + balances.size() + " player balance(s).");
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /** Saves ALL balances (used on shutdown). */
    public void saveAll() {
        if (dataConfig == null) dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        plugin.getShardsManager().getBalanceMap()
                .forEach((uuid, amount) ->
                        dataConfig.set("balances." + uuid.toString(), amount));

        writeFile("All balances");
        dirtyEntries.clear();
    }

    /** Saves only modified entries (used by auto-save task). */
    public void saveDirty() {
        if (dirtyEntries.isEmpty()) return;
        if (dataConfig == null) dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        Set<UUID> snapshot = new HashSet<>(dirtyEntries);
        Map<UUID, Long> balances = plugin.getShardsManager().getBalanceMap();

        snapshot.forEach(uuid -> {
            Long amount = balances.get(uuid);
            if (amount != null) {
                dataConfig.set("balances." + uuid.toString(), amount);
            }
        });

        writeFile(snapshot.size() + " dirty entr" + (snapshot.size() == 1 ? "y" : "ies"));
        dirtyEntries.removeAll(snapshot);
    }

    /** Marks a UUID as having unsaved changes. */
    public void markDirty(UUID uuid) {
        dirtyEntries.add(uuid);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private void writeFile(String description) {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data.yml (" + description + ")!", e);
        }
    }
}
