package dev.reakkz.vshards.utils;

import dev.reakkz.vshards.VShards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Centralised message utility built on Paper's native MiniMessage API.
 *
 * <p>All messages are authored in MiniMessage format (config.yml).
 * Placeholder substitution is done with simple string replacement
 * before parsing, keeping config syntax identical to the user's
 * existing style (e.g. %player%, %amount%).
 *
 * <p>No legacy (&-codes / &#hex) are used anywhere in this plugin.
 */
public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    // -----------------------------------------------------------------------
    // Core parse
    // -----------------------------------------------------------------------

    /**
     * Parses a MiniMessage string (after applying placeholder replacements)
     * into a {@link Component}.
     *
     * @param text         raw MiniMessage string
     * @param replacements alternating %key%, value pairs
     */
    public static Component parse(String text, String... replacements) {
        if (text == null) return Component.empty();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            // Escape the replacement value so player names / amounts can't
            // inject MiniMessage tags (e.g. a player named "<red>evil</red>").
            String safe = MM.escapeTags(replacements[i + 1]);
            text = text.replace(replacements[i], safe);
        }
        return MM.deserialize(text);
    }

    /**
     * Strips all MiniMessage tags and returns plain text.
     * Useful for console logging.
     */
    public static String stripTags(String text) {
        if (text == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(MM.deserialize(text));
    }

    // -----------------------------------------------------------------------
    // Config-driven sending
    // -----------------------------------------------------------------------

    /**
     * Reads a message from {@code messages.<key>} in config.yml,
     * prepends the configured prefix, applies replacements, and sends
     * the result to {@code sender}.
     *
     * @param sender       recipient
     * @param key          config key under {@code messages.*}
     * @param replacements alternating %placeholder%, value pairs
     */
    public static void send(CommandSender sender, String key, String... replacements) {
        String prefix  = cfg("prefix", "");
        String message = cfg(key, "<red>Missing message: " + key + "</red>");
        sender.sendMessage(parse(prefix + message, replacements));
    }

    /**
     * Sends a raw MiniMessage string with the prefix — no config lookup.
     */
    public static void sendRaw(CommandSender sender, String miniMessage, String... replacements) {
        String prefix = cfg("prefix", "");
        sender.sendMessage(parse(prefix + miniMessage, replacements));
    }

    /**
     * Sends a pre-built {@link Component} directly (no prefix).
     */
    public static void sendComponent(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Shorthand config reader for the messages section. */
    private static String cfg(String key, String fallback) {
        return VShards.getInstance().getConfig()
                .getString("messages." + key, fallback);
    }
}
