package dev.reakkz.vshards.tasks;

import dev.reakkz.vshards.VShards;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Asynchronous task that periodically flushes modified player balances to disk.
 * Only dirty (changed) entries are written, keeping I/O minimal.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final VShards plugin;

    public AutoSaveTask(VShards plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getDataManager().saveDirty();
    }
}
