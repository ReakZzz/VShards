package dev.reakkz.vshards.managers;

import dev.reakkz.vshards.VShards;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;

/**
 * Manages in-memory shard balances and exposes all balance operations.
 * All data is keyed by UUID to survive player name changes.
 */
public class ShardsManager {

    private final VShards plugin;
    private final DataManager dataManager;

    // UUID → balance map (the live in-memory store)
    private final Map<UUID, Long> balances;

    public ShardsManager(VShards plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.balances = new HashMap<>();
    }

    // -----------------------------------------------------------------------
    // Balance accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the shard balance for the given UUID.
     * If no entry exists the player receives the configured starting balance.
     */
    public long getBalance(UUID uuid) {
        return balances.computeIfAbsent(uuid, k -> plugin.getConfig().getLong("currency.starting-balance", 0));
    }

    /** Convenience overload for OfflinePlayer. */
    public long getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    // -----------------------------------------------------------------------
    // Balance mutators
    // -----------------------------------------------------------------------

    /**
     * Sets a player's balance to an exact value.
     *
     * @throws IllegalArgumentException if amount is negative or exceeds max balance.
     */
    public void setBalance(UUID uuid, long amount) {
        validateAmount(amount);
        checkMaxBalance(amount);
        balances.put(uuid, amount);
        dataManager.markDirty(uuid);
    }

    public void setBalance(OfflinePlayer player, long amount) {
        setBalance(player.getUniqueId(), amount);
    }

    /**
     * Adds {@code amount} shards to the specified player's balance.
     *
     * @throws IllegalArgumentException if amount <= 0 or the result exceeds max balance.
     */
    public void give(UUID uuid, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        long current = getBalance(uuid);
        long result = current + amount;
        checkMaxBalance(result);
        balances.put(uuid, result);
        dataManager.markDirty(uuid);
    }

    public void give(OfflinePlayer player, long amount) {
        give(player.getUniqueId(), amount);
    }

    /**
     * Removes {@code amount} shards from the specified player's balance.
     *
     * @throws IllegalArgumentException if amount <= 0.
     * @throws IllegalStateException    if the player's balance would drop below 0.
     */
    public void take(UUID uuid, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        long current = getBalance(uuid);
        if (current < amount) throw new IllegalStateException("Insufficient funds.");
        balances.put(uuid, current - amount);
        dataManager.markDirty(uuid);
    }

    public void take(OfflinePlayer player, long amount) {
        take(player.getUniqueId(), amount);
    }

    // -----------------------------------------------------------------------
    // Leaderboard
    // -----------------------------------------------------------------------

    /**
     * Returns an ordered list of UUID→balance entries, highest first.
     * The returned list is a fresh snapshot — safe to iterate.
     */
    public List<Map.Entry<UUID, Long>> getTopBalances() {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .toList();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Exposes the live balance map to {@link DataManager}. */
    Map<UUID, Long> getBalanceMap() {
        return balances;
    }

    private void validateAmount(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative.");
    }

    private void checkMaxBalance(long amount) {
        long max = plugin.getConfig().getLong("currency.max-balance", -1);
        if (max >= 0 && amount > max) {
            throw new IllegalArgumentException("Amount exceeds the maximum balance of " + max + ".");
        }
    }
}
