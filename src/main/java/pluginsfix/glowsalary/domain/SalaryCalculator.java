package pluginsfix.glowsalary.domain;

public final class SalaryCalculator {

    private SalaryCalculator() {}

    public static double calculateMoneySalary(double rankBase, double growthPerClaim, int streak) {
        if (streak <= 0) {
            return rankBase;
        }
        return rankBase + ((long) streak * growthPerClaim);
    }

    public static int calculateSapphireSalary(int baseAmount, int growthPerClaim, int streak) {
        if (streak <= 0) {
            return baseAmount;
        }
        return baseAmount + (streak * growthPerClaim);
    }

    public static long calculateRemainingCooldown(long lastClaimEpochSeconds, long cooldownSeconds, long currentEpochSeconds) {
        if (lastClaimEpochSeconds <= 0 || cooldownSeconds <= 0) {
            return 0L;
        }
        long readyAt = lastClaimEpochSeconds + cooldownSeconds;
        long remaining = readyAt - currentEpochSeconds;
        return Math.max(0L, remaining);
    }

    public static boolean isCooldownActive(long lastClaimEpochSeconds, long cooldownSeconds, long currentEpochSeconds) {
        return calculateRemainingCooldown(lastClaimEpochSeconds, cooldownSeconds, currentEpochSeconds) > 0L;
    }

    public static boolean shouldGiveSapphires(double chancePercent, double rollPercent) {
        if (chancePercent <= 0.0) {
            return false;
        }
        return rollPercent < chancePercent;
    }
}
