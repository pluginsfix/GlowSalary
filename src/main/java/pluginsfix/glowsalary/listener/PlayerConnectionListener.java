package pluginsfix.glowsalary.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pluginsfix.glowsalary.service.SalaryService;

public final class PlayerConnectionListener implements Listener {

    private final SalaryService salaryService;

    public PlayerConnectionListener(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        salaryService.loadPlayerProfile(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        salaryService.unloadPlayer(player.getUniqueId());
    }
}
