package pluginsfix.glowsalary.service;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import pluginsfix.glowsalary.config.SalaryConfig;
import pluginsfix.glowsalary.domain.GroupSalaryConfig;
import pluginsfix.glowsalary.domain.RewardOutcome;
import pluginsfix.glowsalary.domain.SalaryCalculator;
import pluginsfix.glowsalary.domain.SalaryProfile;
import pluginsfix.glowsalary.hook.LuckPermsHook;
import pluginsfix.glowsalary.hook.VaultHook;
import pluginsfix.glowsalary.platform.PlatformScheduler;
import pluginsfix.glowsalary.storage.SalaryRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SalaryService {

    private final SalaryRepository repository;
    private final PlatformScheduler scheduler;
    private final LuckPermsHook luckPermsHook;
    private final VaultHook vaultHook;
    private final Logger logger;
    private volatile SalaryConfig config;

    private final Map<UUID, SalaryProfile> profileCache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyProfiles = ConcurrentHashMap.newKeySet();

    public SalaryService(
        SalaryRepository repository,
        PlatformScheduler scheduler,
        LuckPermsHook luckPermsHook,
        VaultHook vaultHook,
        SalaryConfig initialConfig,
        Logger logger
    ) {
        this.repository = repository;
        this.scheduler = scheduler;
        this.luckPermsHook = luckPermsHook;
        this.vaultHook = vaultHook;
        this.config = initialConfig;
        this.logger = logger;
    }

    public void updateConfig(SalaryConfig newConfig) {
        this.config = newConfig;
    }

    public SalaryConfig getConfig() {
        return config;
    }

    public CompletableFuture<SalaryProfile> loadPlayerProfile(UUID playerId) {
        SalaryProfile cached = profileCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.loadProfile(playerId).thenApply(profile -> {
            profileCache.put(playerId, profile);
            return profile;
        });
    }

    public void unloadPlayer(UUID playerId) {
        SalaryProfile profile = profileCache.remove(playerId);
        if (profile != null && dirtyProfiles.remove(playerId)) {
            repository.saveProfile(profile);
        }
    }

    public RewardOutcome processSalaryClaim(Player player) {
        UUID uuid = player.getUniqueId();
        SalaryProfile profile = profileCache.computeIfAbsent(uuid, SalaryProfile::initial);

        String groupName = luckPermsHook.resolvePrimaryGroup(player, config.groups().keySet());
        GroupSalaryConfig groupConfig = config.resolveGroup(groupName);

        long now = Instant.now().getEpochSecond();
        long remainingCooldown = SalaryCalculator.calculateRemainingCooldown(
            profile.lastClaimEpochSeconds(),
            groupConfig.cooldownSeconds(),
            now
        );

        if (remainingCooldown > 0L) {
            double nextMoney = SalaryCalculator.calculateMoneySalary(groupConfig, profile.moneyStreak());
            return new RewardOutcome.CooldownActive(remainingCooldown, nextMoney, groupConfig.groupName());
        }

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        boolean givesSapphires = SalaryCalculator.shouldGiveSapphires(config.sapphire(), roll);

        if (givesSapphires) {
            int amount = SalaryCalculator.calculateSapphireSalary(config.sapphire(), profile.sapphireStreak());
            SalaryProfile updated = profile.withSapphireClaim(amount, now);
            profileCache.put(uuid, updated);
            dirtyProfiles.add(uuid);

            scheduler.runForPlayer(player, () -> {
                for (String rawCmd : config.sapphire().rewardCommands()) {
                    String formatted = rawCmd
                        .replace("<player>", player.getName())
                        .replace("<amount>", String.valueOf(amount));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
                }

                String soundKey = config.sapphire().sound();
                if (soundKey != null && !soundKey.isBlank()) {
                    try {
                        Key key = Key.key(soundKey);
                        player.playSound(Sound.sound(key, Sound.Source.PLAYER, 1.0f, 1.0f));
                    } catch (Exception e) {
                        logger.warn("Invalid sound key configured: {}", soundKey);
                    }
                }
            });

            return new RewardOutcome.Sapphire(amount, updated.sapphireStreak());
        } else {
            double amount = SalaryCalculator.calculateMoneySalary(groupConfig, profile.moneyStreak());
            SalaryProfile updated = profile.withMoneyClaim(amount, now);
            profileCache.put(uuid, updated);
            dirtyProfiles.add(uuid);

            scheduler.runForPlayer(player, () -> {
                if (vaultHook.isAvailable()) {
                    vaultHook.deposit(player, amount);
                } else {
                    // Fallback to console command if Vault economy is not registered
                    Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "eco give " + player.getName() + " " + (long) amount
                    );
                }
            });

            return new RewardOutcome.Money(amount, updated.moneyStreak());
        }
    }

    public CompletableFuture<Boolean> resetPlayer(UUID playerId) {
        SalaryProfile profile = profileCache.get(playerId);
        if (profile != null) {
            profileCache.put(playerId, profile.withReset());
        }
        dirtyProfiles.remove(playerId);
        return repository.resetProfile(playerId).thenApply(v -> true);
    }

    public SalaryProfile getProfile(UUID playerId) {
        return profileCache.getOrDefault(playerId, SalaryProfile.initial(playerId));
    }

    public void saveDirtyProfiles() {
        if (dirtyProfiles.isEmpty()) {
            return;
        }

        List<SalaryProfile> toSave = new ArrayList<>();
        for (UUID uuid : dirtyProfiles) {
            SalaryProfile profile = profileCache.get(uuid);
            if (profile != null) {
                toSave.add(profile);
            }
        }
        dirtyProfiles.clear();

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
    }

    public void flushAllSync() {
        saveDirtyProfiles();
        repository.close();
    }
}
