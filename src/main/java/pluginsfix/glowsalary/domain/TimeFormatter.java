package pluginsfix.glowsalary.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TimeFormatter {

    private TimeFormatter() {}

    public static String formatDuration(Duration duration) {
        return formatSeconds(duration.getSeconds());
    }

    public static String formatSeconds(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 секунд";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>(4);

        if (days > 0) {
            parts.add(RussianDeclension.formatWithWord(days, "день", "дня", "дней"));
        }
        if (hours > 0) {
            parts.add(RussianDeclension.formatWithWord(hours, "час", "часа", "часов"));
        }
        if (minutes > 0) {
            parts.add(RussianDeclension.formatWithWord(minutes, "минута", "минуты", "минут"));
        }
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(RussianDeclension.formatWithWord(seconds, "секунда", "секунды", "секунд"));
        }

        return String.join(" ", parts);
    }
}
