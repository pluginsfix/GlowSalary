package pluginsfix.glowsalary.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

    private final Economy economy;

    public VaultHook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
            this.economy = provider != null ? provider.getProvider() : null;
        } else {
            this.economy = null;
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || player == null || amount <= 0) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }
}
