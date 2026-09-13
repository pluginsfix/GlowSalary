package pluginsfix.glowsalary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalaryCalculatorTest {

    @Test
    @DisplayName("Базовая зарплата выдаётся при первом получении (streak = 0)")
    void calculatesBaseSalaryForZeroStreak() {
        GroupSalaryConfig group = new GroupSalaryConfig("default", 150.0, 15.0, 600.0, 7200L);
        double salary = SalaryCalculator.calculateMoneySalary(group, 0);

        assertThat(salary).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Зарплата увеличивается пропорционально количеству полученных выплат")
    void calculatesProgressiveSalary() {
        GroupSalaryConfig group = new GroupSalaryConfig("default", 150.0, 15.0, 600.0, 7200L);
        double salaryStreak3 = SalaryCalculator.calculateMoneySalary(group, 3);

        assertThat(salaryStreak3).isEqualTo(195.0); // 150 + 3 * 15 = 195
    }

    @Test
    @DisplayName("Размер зарплаты не превышает максимальный лимит группы")
    void respectsMaxSalaryCap() {
        GroupSalaryConfig group = new GroupSalaryConfig("default", 150.0, 15.0, 600.0, 7200L);
        double salaryStreak100 = SalaryCalculator.calculateMoneySalary(group, 100);

        assertThat(salaryStreak100).isEqualTo(600.0);
    }

    @Test
    @DisplayName("Количество сапфиров растёт с каждым сапфировым получением и ограничено максимумом")
    void calculatesProgressiveSapphires() {
        SapphireRewardConfig sapphire = new SapphireRewardConfig(
            true, 15.0, 1, 1, 10, "entity.player.levelup", List.of("give <player> diamond <amount>")
        );

        assertThat(SalaryCalculator.calculateSapphireSalary(sapphire, 0)).isEqualTo(1);
        assertThat(SalaryCalculator.calculateSapphireSalary(sapphire, 2)).isEqualTo(3);
        assertThat(SalaryCalculator.calculateSapphireSalary(sapphire, 20)).isEqualTo(10);
    }

    @Test
    @DisplayName("Кулдаун корректно рассчитывает оставшиеся секунды")
    void calculatesRemainingCooldown() {
        long lastClaim = 1000L;
        long cooldown = 3600L; // ready at 4600L

        long remainingAt2000 = SalaryCalculator.calculateRemainingCooldown(lastClaim, cooldown, 2000L);
        assertThat(remainingAt2000).isEqualTo(2600L);

        long remainingAt5000 = SalaryCalculator.calculateRemainingCooldown(lastClaim, cooldown, 5000L);
        assertThat(remainingAt5000).isZero();
    }

    @Test
    @DisplayName("Проверка шанса выпадения сапфиров")
    void checksSapphireChance() {
        SapphireRewardConfig sapphire = new SapphireRewardConfig(
            true, 15.0, 1, 1, 10, "entity.player.levelup", List.of()
        );

        assertThat(SalaryCalculator.shouldGiveSapphires(sapphire, 10.0)).isTrue();
        assertThat(SalaryCalculator.shouldGiveSapphires(sapphire, 14.99)).isTrue();
        assertThat(SalaryCalculator.shouldGiveSapphires(sapphire, 15.0)).isFalse();
        assertThat(SalaryCalculator.shouldGiveSapphires(sapphire, 50.0)).isFalse();

        SapphireRewardConfig disabled = new SapphireRewardConfig(
            false, 100.0, 1, 1, 10, "", List.of()
        );
        assertThat(SalaryCalculator.shouldGiveSapphires(disabled, 0.0)).isFalse();
    }
}
