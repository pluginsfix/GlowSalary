package pluginsfix.glowsalary.platform;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PlatformScheduler {

    private final Plugin plugin;
    private final boolean isFolia;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
    }

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    public void runForPlayer(Player player, Runnable task) {
        if (!player.isOnline()) {
            return;
        }

        try {
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runGlobal(Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runTimerAsync(Runnable task, long delaySeconds, long periodSeconds) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> task.run(),
                delaySeconds,
                periodSeconds,
                TimeUnit.SECONDS
            );
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                task,
                delaySeconds * 20L,
                periodSeconds * 20L
            );
        }
    }
}
