package pluginsfix.glowsalary.domain;

import java.util.Objects;

public record GroupSalaryConfig(
    String groupName,
    double baseSalary,
    double incrementPerClaim,
    double maxSalary,
    long cooldownSeconds
) {

    public GroupSalaryConfig {
        Objects.requireNonNull(groupName, "groupName must not be null");
        if (baseSalary < 0) {
            throw new IllegalArgumentException("baseSalary must be >= 0");
        }
        if (incrementPerClaim < 0) {
            throw new IllegalArgumentException("incrementPerClaim must be >= 0");
        }
        if (maxSalary < baseSalary) {
            throw new IllegalArgumentException("maxSalary must be >= baseSalary");
        }
        if (cooldownSeconds < 0) {
            throw new IllegalArgumentException("cooldownSeconds must be >= 0");
        }
    }
}
