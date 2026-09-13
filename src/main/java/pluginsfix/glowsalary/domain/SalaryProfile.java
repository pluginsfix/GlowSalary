package pluginsfix.glowsalary.domain;

import java.util.Objects;
import java.util.UUID;

public record SalaryProfile(
    UUID playerId,
    int moneyStreak,
    int sapphireStreak,
    long lastClaimEpochSeconds,
    double totalMoneyClaimed,
    int totalSapphiresClaimed
) {

    public SalaryProfile {
        Objects.requireNonNull(playerId, "playerId must not be null");
    }

    public static SalaryProfile initial(UUID playerId) {
        return new SalaryProfile(playerId, 0, 0, 0L, 0.0, 0);
    }

    public SalaryProfile withMoneyClaim(double amount, long epochSeconds) {
        return new SalaryProfile(
            playerId,
            moneyStreak + 1,
            sapphireStreak,
            epochSeconds,
            totalMoneyClaimed + amount,
            totalSapphiresClaimed
        );
    }

    public SalaryProfile withSapphireClaim(int amount, long epochSeconds) {
        return new SalaryProfile(
            playerId,
            moneyStreak,
            sapphireStreak + 1,
            epochSeconds,
            totalMoneyClaimed,
            totalSapphiresClaimed + amount
        );
    }

    public SalaryProfile withReset() {
        return new SalaryProfile(playerId, 0, 0, 0L, 0.0, 0);
    }
}
