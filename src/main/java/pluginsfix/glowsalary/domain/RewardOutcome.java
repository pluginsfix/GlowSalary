package pluginsfix.glowsalary.domain;

public sealed interface RewardOutcome {

    record Money(double amount, int newStreak) implements RewardOutcome {}

    record Sapphire(int amount, int newStreak) implements RewardOutcome {}

    record Cooldown(
        long remainingSeconds,
        double nextMoneyAmount,
        int nextSapphireAmount,
        boolean isNextSapphire,
        String rank
    ) implements RewardOutcome {}
}
