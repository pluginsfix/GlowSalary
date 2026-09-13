package pluginsfix.glowsalary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RussianDeclensionTest {

    @Test
    @DisplayName("Корректное склонение русских существительных")
    void testsDeclensionRules() {
        assertThat(RussianDeclension.plural(1, "монета", "монеты", "монет")).isEqualTo("монета");
        assertThat(RussianDeclension.plural(2, "монета", "монеты", "монет")).isEqualTo("монеты");
        assertThat(RussianDeclension.plural(4, "монета", "монеты", "монет")).isEqualTo("монеты");
        assertThat(RussianDeclension.plural(5, "монета", "монеты", "монет")).isEqualTo("монет");
        assertThat(RussianDeclension.plural(11, "монета", "монеты", "монет")).isEqualTo("монет");
        assertThat(RussianDeclension.plural(21, "монета", "монеты", "монет")).isEqualTo("монета");
        assertThat(RussianDeclension.plural(22, "монета", "монеты", "монет")).isEqualTo("монеты");
        assertThat(RussianDeclension.plural(25, "монета", "монеты", "монет")).isEqualTo("монет");
    }
}
