package pluginsfix.glowsalary.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix = "<gradient:#38bdf8:#818cf8><b>Зарплата</b></gradient> <dark_gray>»</dark_gray> ";
    private FileConfiguration messagesYaml;

    public MessageService(FileConfiguration messagesYaml) {
        this.messagesYaml = messagesYaml;
        if (messagesYaml != null && messagesYaml.contains("prefix")) {
            this.prefix = messagesYaml.getString("prefix", prefix);
        }
    }

    public void reload(FileConfiguration newMessagesYaml) {
        this.messagesYaml = newMessagesYaml;
        if (newMessagesYaml != null && newMessagesYaml.contains("prefix")) {
            this.prefix = newMessagesYaml.getString("prefix", prefix);
        }
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (messagesYaml == null) {
            return;
        }

        if (messagesYaml.isList(key)) {
            List<String> lines = messagesYaml.getStringList(key);
            for (String rawLine : lines) {
                sender.sendMessage(parse(rawLine, placeholders));
            }
        } else {
            String rawLine = messagesYaml.getString(key, "<red>Missing message: " + key + "</red>");
            sender.sendMessage(parse(rawLine, placeholders));
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Map.of());
    }

    public Component parse(String rawText, Map<String, String> placeholders) {
        List<TagResolver> resolvers = new ArrayList<>(placeholders.size() + 1);
        resolvers.add(Placeholder.parsed("prefix", prefix));

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }

        return miniMessage.deserialize(rawText, TagResolver.resolver(resolvers));
    }

    public Component parse(String rawText) {
        return parse(rawText, Map.of());
    }
}
