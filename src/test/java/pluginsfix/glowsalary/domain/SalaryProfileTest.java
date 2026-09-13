package pluginsfix.glowsalary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SalaryProfileTest {

    @Test
    @DisplayName("Обновление профиля при получении денег")
    void updatesOnMoneyClaim() {
        UUID id = UUID.randomUUID();
        SalaryProfile initial = SalaryProfile.initial(id);

        assertThat(initial.moneyStreak()).isZero();
        assertThat(initial.sapphireStreak()).isZero();
        assertThat(initial.lastClaimEpochSeconds()).isZero();

        SalaryProfile afterMoney = initial.withMoneyClaim(150.0, 1000L);
        assertThat(afterMoney.moneyStreak()).isEqualTo(1);
        assertThat(afterMoney.totalMoneyClaimed()).isEqualTo(150.0);
        assertThat(afterMoney.lastClaimEpochSeconds()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Обновление профиля при получении сапфиров")
    void updatesOnSapphireClaim() {
        UUID id = UUID.randomUUID();
        SalaryProfile initial = SalaryProfile.initial(id);

        SalaryProfile afterSapphire = initial.withSapphireClaim(2, 2000L);
        assertThat(afterSapphire.sapphireStreak()).isEqualTo(1);
        assertThat(afterSapphire.totalSapphiresClaimed()).isEqualTo(2);
        assertThat(afterSapphire.lastClaimEpochSeconds()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("Сброс профиля")
    void resetsProfile() {
        UUID id = UUID.randomUUID();
        SalaryProfile profile = SalaryProfile.initial(id).withMoneyClaim(100.0, 1000L);

        SalaryProfile reset = profile.withReset();
        assertThat(reset.moneyStreak()).isZero();
        assertThat(reset.sapphireStreak()).isZero();
        assertThat(reset.lastClaimEpochSeconds()).isZero();
        assertThat(reset.totalMoneyClaimed()).isZero();
    }
}
