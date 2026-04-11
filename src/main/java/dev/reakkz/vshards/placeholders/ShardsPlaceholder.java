package dev.reakkz.vshards.placeholders;

import dev.reakkz.vshards.VShards;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the following PlaceholderAPI placeholders:
 *
 *   %vshards_amount%            → the requesting player's balance
 *   %vshards_amount_<player>%   → a named player's balance
 */
public class ShardsPlaceholder extends PlaceholderExpansion {

    private final VShards plugin;

    public ShardsPlaceholder(VShards plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vshards";
    }

    @Override
    public @NotNull String getAuthor() {
        return "reakkz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Keep the expansion registered after a reload. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        // %vshards_amount%
        if (params.equalsIgnoreCase("amount")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getShardsManager().getBalance(player));
        }

        // %vshards_amount_<player>%
        if (params.toLowerCase().startsWith("amount_")) {
            String targetName = params.substring("amount_".length());
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
            if (target == null) return "0";
            return String.valueOf(plugin.getShardsManager().getBalance(target));
        }

        return null; // Unknown placeholder
    }
}
