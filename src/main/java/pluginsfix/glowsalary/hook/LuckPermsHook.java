package pluginsfix.glowsalary.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Comparator;
import java.util.OptionalInt;
import java.util.Set;

public final class LuckPermsHook {

    private final LuckPerms luckPerms;

    public LuckPermsHook() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
        } else {
            this.luckPerms = null;
        }
    }

    public boolean isAvailable() {
        return luckPerms != null;
    }

    public String resolvePrimaryGroup(Player player, Set<String> configuredGroups) {
        if (luckPerms == null || player == null) {
            return "default";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "default";
        }

        // Find matching group with highest LuckPerms weight
        String highestGroup = user.getNodes().stream()
            .filter(node -> node instanceof InheritanceNode)
            .map(node -> ((InheritanceNode) node).getGroupName().toLowerCase())
            .filter(configuredGroups::contains)
            .max(Comparator.comparingInt(this::getGroupWeight))
            .orElse(null);

        if (highestGroup != null) {
            return highestGroup;
        }

        String primaryGroup = user.getPrimaryGroup();
        if (primaryGroup != null && configuredGroups.contains(primaryGroup.toLowerCase())) {
            return primaryGroup.toLowerCase();
        }

        return "default";
    }

    private int getGroupWeight(String groupName) {
        if (luckPerms == null) {
            return 0;
        }
        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return 0;
        }
        OptionalInt weight = group.getWeight();
        return weight.orElse(0);
    }
}
