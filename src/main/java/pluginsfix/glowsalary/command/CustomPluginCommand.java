package pluginsfix.glowsalary.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Objects;

public final class CustomPluginCommand extends Command {

    private final CommandExecutor executor;
    private final TabCompleter completer;

    public CustomPluginCommand(
        String name,
        String description,
        List<String> aliases,
        CommandExecutor executor,
        TabCompleter completer
    ) {
        super(name, description, "/" + name, aliases);
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.completer = completer;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (completer != null) {
            return completer.onTabComplete(sender, this, alias, args);
        }
        return super.tabComplete(sender, alias, args);
    }
}
