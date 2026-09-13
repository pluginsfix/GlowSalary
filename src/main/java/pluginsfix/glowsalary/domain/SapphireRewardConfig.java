package pluginsfix.glowsalary.domain;

import java.util.List;
import java.util.Objects;

public record SapphireRewardConfig(
    boolean enabled,
    double chancePercent,
    int baseAmount,
    int incrementPerClaim,
    int maxAmount,
    String sound,
    List<String> rewardCommands
) {

    public SapphireRewardConfig {
        Objects.requireNonNull(rewardCommands, "rewardCommands must not be null");
        if (chancePercent < 0.0 || chancePercent > 100.0) {
            throw new IllegalArgumentException("chancePercent must be between 0 and 100");
        }
        if (baseAmount < 1) {
            throw new IllegalArgumentException("baseAmount must be >= 1");
        }
        if (incrementPerClaim < 0) {
            throw new IllegalArgumentException("incrementPerClaim must be >= 0");
        }
        if (maxAmount < baseAmount) {
            throw new IllegalArgumentException("maxAmount must be >= baseAmount");
        }
    }
}
