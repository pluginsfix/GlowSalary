package pluginsfix.glowsalary.domain;

public final class SalaryCalculator {

    private SalaryCalculator() {}

    public static double calculateMoneySalary(GroupSalaryConfig groupConfig, int moneyStreak) {
        if (moneyStreak <= 0) {
            return groupConfig.baseSalary();
        }
        double calculated = groupConfig.baseSalary() + (moneyStreak * groupConfig.incrementPerClaim());
        return Math.min(calculated, groupConfig.maxSalary());
    }

    public static int calculateSapphireSalary(SapphireRewardConfig sapphireConfig, int sapphireStreak) {
        if (sapphireStreak <= 0) {
            return sapphireConfig.baseAmount();
        }
        long calculated = (long) sapphireConfig.baseAmount() + ((long) sapphireStreak * sapphireConfig.incrementPerClaim());
        return (int) Math.min(calculated, sapphireConfig.maxAmount());
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

    public static boolean shouldGiveSapphires(SapphireRewardConfig sapphireConfig, double rollPercent) {
        if (!sapphireConfig.enabled()) {
            return false;
        }
        return rollPercent < sapphireConfig.chancePercent();
    }
}
