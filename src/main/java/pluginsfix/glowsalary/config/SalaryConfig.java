package pluginsfix.glowsalary.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record SalaryConfig(
    long cooldownSeconds,
    long initialCooldownSeconds,
    double baseReward,
    double growthPerClaim,
    double sapphireChance,
    int sapphireBaseAmount,
    int sapphireGrowthPerClaim,
    String moneyCommand,
    String sapphireCommand,
    Map<String, Double> ranks
) {

    public SalaryConfig {
        Objects.requireNonNull(moneyCommand, "moneyCommand must not be null");
        Objects.requireNonNull(sapphireCommand, "sapphireCommand must not be null");
        Objects.requireNonNull(ranks, "ranks must not be null");
    }

    public static SalaryConfig fromYaml(FileConfiguration yaml) {
        long cooldownSeconds = yaml.getLong("cooldown.seconds", 3600L);
        long initialCooldownSeconds = yaml.getLong("cooldown.initial-seconds", 3600L);

        double baseReward = yaml.getDouble("rewards.base", 67000.0);
        double growthPerClaim = yaml.getDouble("rewards.growth-per-claim", 15000.0);

        double sapphireChance = yaml.getDouble("sapphire.chance", 10.0);
        int sapphireBase = yaml.getInt("sapphire.base-amount", 5);
        int sapphireGrowth = yaml.getInt("sapphire.growth-per-claim", 2);

        String moneyCommand = yaml.getString("commands.money", "eco give %player% %amount%");
        String sapphireCommand = yaml.getString("commands.sapphire", "p give %player% %amount%");

        Map<String, Double> rankMap = new HashMap<>();
        ConfigurationSection ranksSection = yaml.getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rankKey : ranksSection.getKeys(false)) {
                rankMap.put(rankKey.toLowerCase(), ranksSection.getDouble(rankKey));
            }
        }

        if (!rankMap.containsKey("default")) {
            rankMap.put("default", baseReward);
        }

        return new SalaryConfig(
            cooldownSeconds,
            initialCooldownSeconds,
            baseReward,
            growthPerClaim,
            sapphireChance,
            sapphireBase,
            sapphireGrowth,
            moneyCommand,
            sapphireCommand,
            Collections.unmodifiableMap(rankMap)
        );
    }

    public double resolveRankBase(String rankName) {
        if (rankName == null) {
            return ranks.getOrDefault("default", baseReward);
        }
        return ranks.getOrDefault(rankName.toLowerCase(), ranks.getOrDefault("default", baseReward));
    }
}
