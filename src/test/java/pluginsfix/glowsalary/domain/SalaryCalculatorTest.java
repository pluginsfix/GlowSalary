package pluginsfix.glowsalary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalaryCalculatorTest {

    @Test
    @DisplayName("Базовая зарплата выдаётся при первом получении (streak = 0)")
    void calculatesBaseSalaryForZeroStreak() {
        double salary = SalaryCalculator.calculateMoneySalary(67000.0, 15000.0, 0);
        assertThat(salary).isEqualTo(67000.0);
    }

    @Test
    @DisplayName("Зарплата увеличивается пропорционально количеству полученных выплат")
    void calculatesProgressiveSalary() {
        double salaryStreak3 = SalaryCalculator.calculateMoneySalary(67000.0, 15000.0, 3);
        assertThat(salaryStreak3).isEqualTo(112000.0);
    }

    @Test
    @DisplayName("Расчет зарплаты для донат-ранга")
    void calculatesRankSalary() {
        double salaryGrieferStreak2 = SalaryCalculator.calculateMoneySalary(100000.0, 15000.0, 2);
        assertThat(salaryGrieferStreak2).isEqualTo(130000.0);
    }

    @Test
    @DisplayName("Количество сапфиров растёт с каждым получением")
    void calculatesProgressiveSapphires() {
        assertThat(SalaryCalculator.calculateSapphireSalary(5, 2, 0)).isEqualTo(5);
        assertThat(SalaryCalculator.calculateSapphireSalary(5, 2, 1)).isEqualTo(7);
        assertThat(SalaryCalculator.calculateSapphireSalary(5, 2, 3)).isEqualTo(11);
    }

    @Test
    @DisplayName("Кулдаун корректно рассчитывает оставшиеся секунды")
    void calculatesRemainingCooldown() {
        long lastClaim = 1000L;
        long cooldown = 3600L;

        long remainingAt2000 = SalaryCalculator.calculateRemainingCooldown(lastClaim, cooldown, 2000L);
        assertThat(remainingAt2000).isEqualTo(2600L);

        long remainingAt5000 = SalaryCalculator.calculateRemainingCooldown(lastClaim, cooldown, 5000L);
        assertThat(remainingAt5000).isZero();
    }

    @Test
    @DisplayName("Проверка шанса выпадения сапфиров")
    void checksSapphireChance() {
        assertThat(SalaryCalculator.shouldGiveSapphires(10.0, 5.0)).isTrue();
        assertThat(SalaryCalculator.shouldGiveSapphires(10.0, 9.99)).isTrue();
        assertThat(SalaryCalculator.shouldGiveSapphires(10.0, 10.0)).isFalse();
        assertThat(SalaryCalculator.shouldGiveSapphires(10.0, 50.0)).isFalse();
        assertThat(SalaryCalculator.shouldGiveSapphires(0.0, 0.0)).isFalse();
    }
}
