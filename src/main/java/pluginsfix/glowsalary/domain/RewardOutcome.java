package pluginsfix.glowsalary.domain;

public sealed interface RewardOutcome {

    record Money(double amount, int newStreak) implements RewardOutcome {}

    record Sapphire(int amount, int newStreak) implements RewardOutcome {}

    record CooldownActive(long remainingSeconds, double nextMoneyAmount, String groupName) implements RewardOutcome {}
}
