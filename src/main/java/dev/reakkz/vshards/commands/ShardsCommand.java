package dev.reakkz.vshards.commands;

import dev.reakkz.vshards.VShards;
import dev.reakkz.vshards.managers.ShardsManager;
import dev.reakkz.vshards.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all /shards sub-commands with permission checks and tab completion.
 */
public class ShardsCommand implements CommandExecutor, TabCompleter {

    private final VShards plugin;
    private final ShardsManager shardsManager;

    private static final List<String> SUB_COMMANDS = List.of("give", "take", "set", "top", "shop", "reload");
    private static final List<String> AMOUNT_SUGGESTIONS = List.of("100", "500", "1000", "5000", "10000");

    public ShardsCommand(VShards plugin) {
        this.plugin = plugin;
        this.shardsManager = plugin.getShardsManager();
    }

    // -----------------------------------------------------------------------
    // Execution
    // -----------------------------------------------------------------------

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // /shards — check own balance
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, "error-player-only");
                return true;
            }
            if (!player.hasPermission("vshards.balance")) {
                MessageUtil.send(sender, "error-no-permission");
                return true;
            }
            long balance = shardsManager.getBalance(player);
            MessageUtil.send(sender, "balance-self", "%amount%", formatNumber(balance));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        return switch (sub) {
            case "give"   -> handleAdminOp(sender, args, "give");
            case "take"   -> handleAdminOp(sender, args, "take");
            case "set"    -> handleAdminOp(sender, args, "set");
            case "top"    -> handleTop(sender, args);
            case "shop"   -> handleShop(sender);
            case "reload" -> handleReload(sender);
            default       -> handleBalanceOther(sender, args[0]);
        };
    }

    // -----------------------------------------------------------------------
    // Sub-command handlers
    // -----------------------------------------------------------------------

    /** /shards <player> — check another player's balance */
    private boolean handleBalanceOther(CommandSender sender, String targetName) {
        if (!sender.hasPermission("vshards.balance.others")) {
            MessageUtil.send(sender, "error-no-permission");
            return true;
        }
        OfflinePlayer target = resolveOfflinePlayer(targetName);
        if (target == null) {
            MessageUtil.send(sender, "error-player-not-found", "%player%", targetName);
            return true;
        }
        long balance = shardsManager.getBalance(target);
        MessageUtil.send(sender, "balance-other",
                "%player%", Objects.requireNonNullElse(target.getName(), targetName),
                "%amount%", formatNumber(balance));
        return true;
    }

    /** /shards give|take|set <player> <amount> */
    private boolean handleAdminOp(CommandSender sender, String[] args, String op) {
        String permission = "vshards.admin." + op;
        if (!sender.hasPermission(permission)) {
            MessageUtil.send(sender, "error-no-permission");
            return true;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "error-usage", "%usage%",
                    "/shards " + op + " <player> <amount>");
            return true;
        }

        String targetName = args[1];
        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "error-invalid-amount");
            return true;
        }

        OfflinePlayer target = resolveOfflinePlayer(targetName);
        if (target == null) {
            MessageUtil.send(sender, "error-player-not-found", "%player%", targetName);
            return true;
        }

        String displayName = Objects.requireNonNullElse(target.getName(), targetName);
        String senderName = sender instanceof Player p ? p.getName() : "Console";

        try {
            switch (op) {
                case "give" -> {
                    shardsManager.give(target, amount);
                    MessageUtil.send(sender, "give-success",
                            "%amount%", formatNumber(amount),
                            "%player%", displayName);
                    notifyPlayer(target, "give-received",
                            "%amount%", formatNumber(amount),
                            "%sender%", senderName);
                }
                case "take" -> {
                    shardsManager.take(target, amount);
                    MessageUtil.send(sender, "take-success",
                            "%amount%", formatNumber(amount),
                            "%player%", displayName);
                    notifyPlayer(target, "take-received",
                            "%amount%", formatNumber(amount),
                            "%sender%", senderName);
                }
                case "set" -> {
                    shardsManager.setBalance(target, amount);
                    MessageUtil.send(sender, "set-success",
                            "%amount%", formatNumber(amount),
                            "%player%", displayName);
                    notifyPlayer(target, "set-received",
                            "%amount%", formatNumber(amount),
                            "%sender%", senderName);
                }
            }
        } catch (IllegalStateException e) {
            MessageUtil.send(sender, "error-insufficient-funds");
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "error-invalid-amount");
        }
        return true;
    }

    /** /shards shop — open the shop GUI */
    private boolean handleShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "error-player-only");
            return true;
        }
        if (!player.hasPermission("vshards.shop")) {
            MessageUtil.send(sender, "error-no-permission");
            return true;
        }
        if (!plugin.getShopManager().isEnabled()) {
            MessageUtil.send(sender, "shop-disabled");
            return true;
        }
        plugin.getShopGUI().open(player);
        return true;
    }

    /** /shards top [page] */
    private boolean handleTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vshards.top")) {
            MessageUtil.send(sender, "error-no-permission");
            return true;
        }

        int pageSize = plugin.getConfig().getInt("leaderboard.entries-per-page", 10);
        List<Map.Entry<UUID, Long>> allEntries = shardsManager.getTopBalances();
        int totalPages = Math.max(1, (int) Math.ceil((double) allEntries.size() / pageSize));

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1 || page > totalPages) {
                    MessageUtil.send(sender, "error-invalid-page");
                    return true;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(sender, "error-invalid-page");
                return true;
            }
        }

        // Header
        String header = plugin.getConfig().getString("messages.leaderboard-header",
                "&b--- Shard Leaderboard [%page%/%maxpage%] ---");
        sender.sendMessage(MessageUtil.parse(
                header,
                "%page%", String.valueOf(page),
                "%maxpage%", String.valueOf(totalPages)));

        // Entries
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allEntries.size());

        if (startIndex >= allEntries.size()) {
            MessageUtil.send(sender, "leaderboard-empty");
        } else {
            String entryFormat = plugin.getConfig().getString("messages.leaderboard-entry",
                    " %rank%. %player% — %amount% shards");
            for (int i = startIndex; i < endIndex; i++) {
                Map.Entry<UUID, Long> entry = allEntries.get(i);
                String playerName = Optional.ofNullable(Bukkit.getOfflinePlayer(entry.getKey()).getName())
                        .orElse(entry.getKey().toString().substring(0, 8) + "…");
                sender.sendMessage(MessageUtil.parse(entryFormat,
                        "%rank%", String.valueOf(i + 1),
                        "%player%", playerName,
                        "%amount%", formatNumber(entry.getValue())));
            }
        }

        // Footer
        String footer = plugin.getConfig().getString("messages.leaderboard-footer", "");
        if (!footer.isEmpty()) {
            sender.sendMessage(MessageUtil.parse(footer));
        }
        return true;
    }

    /** /shards reload */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("vshards.admin.reload")) {
            MessageUtil.send(sender, "error-no-permission");
            return true;
        }
        plugin.reload();
        MessageUtil.send(sender, "reload-success");
        return true;
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            // Online player names (for balance lookup)
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            // Sub-commands (filtered by permission)
            if (sender.hasPermission("vshards.admin.give"))   options.add("give");
            if (sender.hasPermission("vshards.admin.take"))   options.add("take");
            if (sender.hasPermission("vshards.admin.set"))    options.add("set");
            if (sender.hasPermission("vshards.top"))          options.add("top");
            if (sender.hasPermission("vshards.shop"))        options.add("shop");
            if (sender.hasPermission("vshards.admin.reload")) options.add("reload");
            return filterByPrefix(options, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Set.of("give", "take", "set").contains(sub)) {
                return filterByPrefix(onlinePlayerNames(), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Set.of("give", "take", "set").contains(sub)) {
                return filterByPrefix(AMOUNT_SUGGESTIONS, args[2]);
            }
        }

        return List.of();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves an OfflinePlayer by name. Checks online players first,
     * then falls back to Bukkit's offline player cache.
     * Returns null if the player was never seen on this server.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveOfflinePlayer(String name) {
        // Check online players first (most efficient)
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        // Fall back to offline lookup
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) return offline;

        return null;
    }

    private void notifyPlayer(OfflinePlayer target, String key, String... replacements) {
        Player online = target.getPlayer();
        if (online != null) {
            MessageUtil.send(online, key, replacements);
        }
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterByPrefix(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .sorted()
                .collect(Collectors.toList());
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
}
