package pluginsfix.glowsalary.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import pluginsfix.glowsalary.config.SalaryConfig;
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

        String rankName = luckPermsHook.resolvePrimaryGroup(player, config.ranks().keySet());
        double rankBase = config.resolveRankBase(rankName);

        long now = Instant.now().getEpochSecond();
        long cooldownDuration = profile.lastClaimEpochSeconds() == 0L
            ? config.initialCooldownSeconds()
            : config.cooldownSeconds();

        long remainingCooldown = SalaryCalculator.calculateRemainingCooldown(
            profile.lastClaimEpochSeconds(),
            cooldownDuration,
            now
        );

        if (remainingCooldown > 0L) {
            double nextMoney = SalaryCalculator.calculateMoneySalary(rankBase, config.growthPerClaim(), profile.moneyStreak());
            int nextSapphire = SalaryCalculator.calculateSapphireSalary(config.sapphireBaseAmount(), config.sapphireGrowthPerClaim(), profile.sapphireStreak());
            double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
            boolean isNextSapphire = SalaryCalculator.shouldGiveSapphires(config.sapphireChance(), roll);

            return new RewardOutcome.Cooldown(remainingCooldown, nextMoney, nextSapphire, isNextSapphire, rankName);
        }

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        boolean givesSapphires = SalaryCalculator.shouldGiveSapphires(config.sapphireChance(), roll);

        if (givesSapphires) {
            int amount = SalaryCalculator.calculateSapphireSalary(config.sapphireBaseAmount(), config.sapphireGrowthPerClaim(), profile.sapphireStreak());
            SalaryProfile updated = profile.withSapphireClaim(amount, now);
            profileCache.put(uuid, updated);
            dirtyProfiles.add(uuid);

            scheduler.runForPlayer(player, () -> {
                String cmd = config.sapphireCommand()
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("<player>", player.getName())
                    .replace("<amount>", String.valueOf(amount));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            });

            return new RewardOutcome.Sapphire(amount, updated.sapphireStreak());
        } else {
            double amount = SalaryCalculator.calculateMoneySalary(rankBase, config.growthPerClaim(), profile.moneyStreak());
            SalaryProfile updated = profile.withMoneyClaim(amount, now);
            profileCache.put(uuid, updated);
            dirtyProfiles.add(uuid);

            scheduler.runForPlayer(player, () -> {
                String cmd = config.moneyCommand()
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf((long) amount))
                    .replace("<player>", player.getName())
                    .replace("<amount>", String.valueOf((long) amount));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
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
