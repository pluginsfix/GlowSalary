package pluginsfix.glowsalary.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pluginsfix.glowsalary.domain.RewardOutcome;
import pluginsfix.glowsalary.domain.SalaryProfile;
import pluginsfix.glowsalary.domain.TimeFormatter;
import pluginsfix.glowsalary.service.SalaryService;
import pluginsfix.glowsalary.text.MessageService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SalaryCommand implements CommandExecutor, TabCompleter {

    private final SalaryService salaryService;
    private final MessageService messageService;
    private final Runnable reloadAction;

    public SalaryCommand(SalaryService salaryService, MessageService messageService, Runnable reloadAction) {
        this.salaryService = salaryService;
        this.messageService = messageService;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            handleClaim(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> messageService.sendMessage(sender, "unknown-command");
        }

        return true;
    }

    private void handleClaim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.sendMessage(sender, "only-player");
            return;
        }

        if (!player.hasPermission("glowsalary.use")) {
            messageService.sendMessage(sender, "no-permission");
            return;
        }

        RewardOutcome outcome = salaryService.processSalaryClaim(player);
        switch (outcome) {
            case RewardOutcome.Money money -> {
                String formattedAmount = formatNumber(money.amount());
                messageService.sendMessage(player, "money-received", Map.of(
                    "player", player.getName(),
                    "amount", formattedAmount,
                    "streak", String.valueOf(money.newStreak())
                ));
            }
            case RewardOutcome.Sapphire sapphire -> {
                messageService.sendMessage(player, "sapphire-received", Map.of(
                    "player", player.getName(),
                    "amount", String.valueOf(sapphire.amount()),
                    "streak", String.valueOf(sapphire.newStreak())
                ));
            }
            case RewardOutcome.Cooldown cd -> {
                String formattedTime = TimeFormatter.formatSeconds(cd.remainingSeconds());
                if (cd.isNextSapphire()) {
                    messageService.sendMessage(player, "not-ready-sapphire", Map.of(
                        "time", formattedTime,
                        "amount", String.valueOf(cd.nextSapphireAmount()),
                        "player", player.getName(),
                        "rank", cd.rank()
                    ));
                } else {
                    String formattedNext = formatNumber(cd.nextMoneyAmount());
                    messageService.sendMessage(player, "not-ready-money", Map.of(
                        "time", formattedTime,
                        "amount", formattedNext,
                        "player", player.getName(),
                        "rank", cd.rank()
                    ));
                }
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("glowsalary.admin")) {
            messageService.sendMessage(sender, "no-permission");
            return;
        }

        reloadAction.run();
        messageService.sendMessage(sender, "reload-success");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("glowsalary.admin")) {
            messageService.sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messageService.sendMessage(sender, "unknown-command");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            salaryService.resetPlayer(target.getUniqueId()).thenAccept(success -> {
                messageService.sendMessage(sender, "reset-success", Map.of("player", target.getName()));
            });
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            salaryService.resetPlayer(offline.getUniqueId()).thenAccept(success -> {
                messageService.sendMessage(sender, "reset-success", Map.of("player", targetName));
            });
        } else {
            messageService.sendMessage(sender, "player-not-found", Map.of("player", targetName));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player targetPlayer;
        if (args.length > 1) {
            if (!sender.hasPermission("glowsalary.admin")) {
                messageService.sendMessage(sender, "no-permission");
                return;
            }
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                messageService.sendMessage(sender, "player-not-found", Map.of("player", args[1]));
                return;
            }
        } else {
            if (!(sender instanceof Player player)) {
                messageService.sendMessage(sender, "only-player");
                return;
            }
            if (!player.hasPermission("glowsalary.use")) {
                messageService.sendMessage(sender, "no-permission");
                return;
            }
            targetPlayer = player;
        }

        SalaryProfile profile = salaryService.getProfile(targetPlayer.getUniqueId());
        long now = Instant.now().getEpochSecond();
        long diff = now - profile.lastClaimEpochSeconds();

        sender.sendMessage(messageService.format("&#FF7000▶&f Информация о зарплате игрока &#FFC900" + targetPlayer.getName() + "&f:"));
        sender.sendMessage(messageService.format("&7• Денежный уровень: &#FFC900#" + profile.moneyStreak() + " &7(Всего: &#FFC900" + formatNumber(profile.totalMoneyClaimed()) + " ¤&7)"));
        sender.sendMessage(messageService.format("&7• Сапфировый уровень: &#fb0fd4#" + profile.sapphireStreak() + " &7(Всего сапфиров: &#fb0fd4" + profile.totalSapphiresClaimed() + " ☀&7)"));
        sender.sendMessage(messageService.format("&7• Прошло с последней выплаты: &#FF7000" + TimeFormatter.formatSeconds(diff)));
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("glowsalary.admin")) {
                completions.add("reload");
                completions.add("reset");
                completions.add("info");
            } else if (sender.hasPermission("glowsalary.use")) {
                completions.add("info");
            }
            return filterPrefix(completions, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("glowsalary.admin")) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reset") || sub.equals("info")) {
                List<String> playerNames = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playerNames.add(p.getName());
                }
                return filterPrefix(playerNames, args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return list.stream()
            .filter(item -> item.toLowerCase(Locale.ROOT).startsWith(lower))
            .sorted()
            .toList();
    }
}
