package pluginsfix.glowsalary.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageService {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)(?:#&|&#)([0-9a-fA-F]{6})");
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();

    private FileConfiguration configYaml;

    public MessageService(FileConfiguration configYaml) {
        this.configYaml = configYaml;
    }

    public void reload(FileConfiguration newConfigYaml) {
        this.configYaml = newConfigYaml;
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (configYaml == null) {
            return;
        }

        String raw = configYaml.getString("messages." + key, "");
        if (raw.isEmpty()) {
            raw = configYaml.getString(key, "&cMissing message: " + key);
        }

        sender.sendMessage(format(raw, placeholders));
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Map.of());
    }

    public Component format(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        String text = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
            text = text.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder b = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                b.append('&').append(c);
            }
            matcher.appendReplacement(sb, b.toString());
        }
        matcher.appendTail(sb);

        return SERIALIZER.deserialize(sb.toString());
    }

    public Component format(String raw) {
        return format(raw, Map.of());
    }
}
