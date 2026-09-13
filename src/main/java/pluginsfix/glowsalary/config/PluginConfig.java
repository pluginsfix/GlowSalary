package pluginsfix.glowsalary.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pluginsfix.glowsalary.domain.GroupSalaryConfig;
import pluginsfix.glowsalary.domain.SapphireRewardConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PluginConfig(
    int configVersion,
    long defaultCooldownSeconds,
    int autoSaveIntervalMinutes,
    SapphireRewardConfig sapphire,
    Map<String, GroupSalaryConfig> groups
) {

    public PluginConfig {
        Objects.requireNonNull(sapphire, "sapphire must not be null");
        Objects.requireNonNull(groups, "groups must not be null");
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("At least one group must be configured in 'groups'");
        }
    }

    public static PluginConfig fromYaml(FileConfiguration yaml) {
        int configVersion = yaml.getInt("config-version", 1);
        long defaultCooldownSeconds = yaml.getLong("default-cooldown-seconds", 7200L);
        int autoSaveIntervalMinutes = yaml.getInt("auto-save-interval-minutes", 5);

        ConfigurationSection sapphireSection = yaml.getConfigurationSection("sapphire");
        SapphireRewardConfig sapphire;
        if (sapphireSection != null) {
            boolean enabled = sapphireSection.getBoolean("enabled", true);
            double chance = sapphireSection.getDouble("chance-percent", 15.0);
            int baseAmount = sapphireSection.getInt("base-amount", 1);
            int increment = sapphireSection.getInt("increment-per-claim", 1);
            int maxAmount = sapphireSection.getInt("max-amount", 10);
            String sound = sapphireSection.getString("sound", "entity.player.levelup");
            List<String> rewardCommands = sapphireSection.getStringList("reward-commands");

            sapphire = new SapphireRewardConfig(enabled, chance, baseAmount, increment, maxAmount, sound, rewardCommands);
        } else {
            sapphire = new SapphireRewardConfig(false, 0.0, 1, 0, 1, "", List.of());
        }

        Map<String, GroupSalaryConfig> groupMap = new HashMap<>();
        ConfigurationSection groupsSection = yaml.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String groupKey : groupsSection.getKeys(false)) {
                ConfigurationSection sec = groupsSection.getConfigurationSection(groupKey);
                if (sec == null) {
                    continue;
                }

                double baseSalary = sec.getDouble("base-salary", 100.0);
                double increment = sec.getDouble("increment-per-claim", 10.0);
                double maxSalary = sec.getDouble("max-salary", Math.max(baseSalary, 500.0));
                long cooldown = sec.getLong("cooldown-seconds", 0L);
                if (cooldown <= 0L) {
                    cooldown = defaultCooldownSeconds;
                }

                groupMap.put(groupKey.toLowerCase(), new GroupSalaryConfig(groupKey, baseSalary, increment, maxSalary, cooldown));
            }
        }

        if (!groupMap.containsKey("default")) {
            groupMap.put("default", new GroupSalaryConfig("default", 100.0, 10.0, 500.0, defaultCooldownSeconds));
        }

        return new PluginConfig(
            configVersion,
            defaultCooldownSeconds,
            autoSaveIntervalMinutes,
            sapphire,
            Collections.unmodifiableMap(groupMap)
        );
    }

    public GroupSalaryConfig resolveGroup(String groupName) {
        if (groupName == null) {
            return groups.getOrDefault("default", groups.values().iterator().next());
        }
        GroupSalaryConfig found = groups.get(groupName.toLowerCase());
        if (found != null) {
            return found;
        }
        return groups.getOrDefault("default", groups.values().iterator().next());
    }
}
