package pluginsfix.glowsalary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeFormatterTest {

    @Test
    @DisplayName("Форматирование нулевого или отрицательного времени")
    void formatsZeroSeconds() {
        assertThat(TimeFormatter.formatSeconds(0)).isEqualTo("0 секунд");
        assertThat(TimeFormatter.formatSeconds(-10)).isEqualTo("0 секунд");
    }

    @Test
    @DisplayName("Форматирование секунд")
    void formatsSecondsOnly() {
        assertThat(TimeFormatter.formatSeconds(1)).isEqualTo("1 секунда");
        assertThat(TimeFormatter.formatSeconds(2)).isEqualTo("2 секунды");
        assertThat(TimeFormatter.formatSeconds(5)).isEqualTo("5 секунд");
    }

    @Test
    @DisplayName("Форматирование минут и секунд")
    void formatsMinutesAndSeconds() {
        assertThat(TimeFormatter.formatSeconds(65)).isEqualTo("1 минута 5 секунд");
        assertThat(TimeFormatter.formatSeconds(122)).isEqualTo("2 минуты 2 секунды");
    }

    @Test
    @DisplayName("Форматирование часов, минут и дней")
    void formatsComplexDuration() {
        assertThat(TimeFormatter.formatSeconds(3665)).isEqualTo("1 час 1 минута 5 секунд");
        assertThat(TimeFormatter.formatSeconds(90000)).isEqualTo("1 день 1 час");
    }
}
