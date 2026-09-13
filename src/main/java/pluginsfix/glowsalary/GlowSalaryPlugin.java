package pluginsfix.glowsalary;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import pluginsfix.glowsalary.command.CustomPluginCommand;
import pluginsfix.glowsalary.command.SalaryCommand;
import pluginsfix.glowsalary.config.PluginConfig;
import pluginsfix.glowsalary.hook.LuckPermsHook;
import pluginsfix.glowsalary.hook.VaultHook;
import pluginsfix.glowsalary.listener.PlayerConnectionListener;
import pluginsfix.glowsalary.platform.PlatformScheduler;
import pluginsfix.glowsalary.service.SalaryService;
import pluginsfix.glowsalary.storage.SqliteSalaryRepository;
import pluginsfix.glowsalary.text.MessageService;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public final class GlowSalaryPlugin extends JavaPlugin {

    private PlatformScheduler scheduler;
    private SqliteSalaryRepository repository;
    private SalaryService salaryService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();
        logger.info("Enabling GlowSalary by pluginsfix...");

        saveDefaultConfig();
        saveDefaultMessages();

        PluginConfig pluginConfig = PluginConfig.fromYaml(getConfig());
        FileConfiguration messagesYaml = loadMessagesYaml();
        this.messageService = new MessageService(messagesYaml);

        this.scheduler = new PlatformScheduler(this);

        Path dbPath = getDataFolder().toPath().resolve("glowsalary.db");
        this.repository = new SqliteSalaryRepository(dbPath, logger);

        LuckPermsHook luckPermsHook = new LuckPermsHook();
        if (luckPermsHook.isAvailable()) {
            logger.info("LuckPerms detected! Group-based salaries enabled.");
        } else {
            logger.warn("LuckPerms not found! Using default group configuration.");
        }

        VaultHook vaultHook = new VaultHook();
        if (vaultHook.isAvailable()) {
            logger.info("Vault Economy detected! Direct economy deposits enabled.");
        } else {
            logger.warn("Vault Economy not found! Fallback to console command for money payments.");
        }

        this.salaryService = new SalaryService(
            repository,
            scheduler,
            luckPermsHook,
            vaultHook,
            pluginConfig,
            logger
        );

        // Register event listener
        getServer().getPluginManager().registerEvents(
            new PlayerConnectionListener(salaryService),
            this
        );

        // Preload any currently online players (useful on reload/hot-load)
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            salaryService.loadPlayerProfile(onlinePlayer.getUniqueId());
        }

        // Register /salary and /glowsalary commands
        SalaryCommand salaryCommand = new SalaryCommand(salaryService, messageService, this::reloadPlugin);
        CustomPluginCommand command = new CustomPluginCommand(
            "salary",
            "Получить регулярную зарплату",
            List.of("glowsalary"),
            salaryCommand,
            salaryCommand
        );
        Bukkit.getCommandMap().register(getName(), command);

        // Setup auto-save task
        long autoSaveSeconds = Math.max(60L, pluginConfig.autoSaveIntervalMinutes() * 60L);
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

    private void reloadPlugin() {
        reloadConfig();
        PluginConfig newConfig = PluginConfig.fromYaml(getConfig());
        salaryService.updateConfig(newConfig);

        FileConfiguration messagesYaml = loadMessagesYaml();
        messageService.reload(messagesYaml);
    }

    private void saveDefaultMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
    }

    private FileConfiguration loadMessagesYaml() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveDefaultMessages();
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
