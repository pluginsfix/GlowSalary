package pluginsfix.glowsalary;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import pluginsfix.glowsalary.command.CustomSalaryCommand;
import pluginsfix.glowsalary.command.SalaryCommand;
import pluginsfix.glowsalary.config.SalaryConfig;
import pluginsfix.glowsalary.hook.LuckPermsHook;
import pluginsfix.glowsalary.hook.VaultHook;
import pluginsfix.glowsalary.listener.PlayerConnectionListener;
import pluginsfix.glowsalary.platform.PlatformScheduler;
import pluginsfix.glowsalary.service.SalaryService;
import pluginsfix.glowsalary.storage.SqliteSalaryRepository;
import pluginsfix.glowsalary.text.MessageService;

import java.nio.file.Path;
import java.util.List;

public final class GlowSalary extends JavaPlugin {

    private PlatformScheduler scheduler;
    private SqliteSalaryRepository repository;
    private SalaryService salaryService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();
        logger.info("Enabling GlowSalary by pluginsfix...");

        saveDefaultConfig();

        SalaryConfig config = SalaryConfig.fromYaml(getConfig());
        this.messageService = new MessageService(getConfig());

        this.scheduler = new PlatformScheduler(this);

        Path dbPath = getDataFolder().toPath().resolve("glowsalary.db");
        this.repository = new SqliteSalaryRepository(dbPath, logger);

        LuckPermsHook luckPermsHook = new LuckPermsHook();
        if (luckPermsHook.isAvailable()) {
            logger.info("LuckPerms detected! Rank-based salaries enabled.");
        } else {
            logger.warn("LuckPerms not found! Using default rank configuration.");
        }

        VaultHook vaultHook = new VaultHook();
        if (vaultHook.isAvailable()) {
            logger.info("Vault Economy detected!");
        }

        this.salaryService = new SalaryService(
            repository,
            scheduler,
            luckPermsHook,
            vaultHook,
            config,
            logger
        );

        getServer().getPluginManager().registerEvents(
            new PlayerConnectionListener(salaryService),
            this
        );

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            salaryService.loadPlayerProfile(onlinePlayer.getUniqueId());
        }

        SalaryCommand salaryCommand = new SalaryCommand(salaryService, messageService, this::reload);
        CustomSalaryCommand command = new CustomSalaryCommand(
            "salary",
            "Получить регулярную зарплату",
            List.of("glowsalary"),
            salaryCommand,
            salaryCommand
        );
        Bukkit.getCommandMap().register(getName(), command);

        long autoSaveSeconds = 300L;
        scheduler.runTimerAsync(salaryService::saveDirtyProfiles, autoSaveSeconds, autoSaveSeconds);

        logger.info("GlowSalary v{} successfully enabled! Platform: {}",
            getPluginMeta().getVersion(),
            scheduler.isFolia() ? "Folia" : "Paper"
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling GlowSalary and saving pending data...");
        if (salaryService != null) {
            salaryService.flushAllSync();
        }
        getLogger().info("GlowSalary successfully disabled.");
    }

    private void reload() {
        reloadConfig();
        SalaryConfig newConfig = SalaryConfig.fromYaml(getConfig());
        salaryService.updateConfig(newConfig);
        messageService.reload(getConfig());
    }
}
