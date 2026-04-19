package dev.nexus.hub.command;

import dev.nexus.api.minion.Minion;
import dev.nexus.api.turret.Turret;
import dev.nexus.api.turret.TurretStatus;
import dev.nexus.hub.minion.MinionManager;
import dev.nexus.hub.minion.MinionRegistry;
import dev.nexus.hub.queue.QueueManager;
import dev.nexus.hub.turret.TurretRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the {@code /nexus} command and all its subcommands.
 *
 * <p>Player subcommands: {@code join}, {@code leave}, {@code status}<br>
 * Admin subcommands (require {@code nexus.admin}): {@code admin list}, {@code admin turrets},
 * {@code admin drain}, {@code admin undrain}, {@code admin kill}
 *
 * @since 1.0.0
 */
public class NexusCommand implements CommandExecutor, TabCompleter {

    private final QueueManager queueManager;
    private final MinionRegistry minionRegistry;
    private final MinionManager minionManager;
    private final TurretRegistry turretRegistry;

    /**
     * @param queueManager    for queue join/leave/status operations
     * @param minionRegistry  for listing live minions
     * @param minionManager   for force-killing minions
     * @param turretRegistry  for drain/undrain and status
     */
    public NexusCommand(QueueManager queueManager, MinionRegistry minionRegistry,
                        MinionManager minionManager, TurretRegistry turretRegistry) {
        this.queueManager = queueManager;
        this.minionRegistry = minionRegistry;
        this.minionManager = minionManager;
        this.turretRegistry = turretRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6Nexus §7— use /nexus join|leave|status|admin");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "status" -> handleStatus(sender);
            case "admin" -> handleAdmin(sender, args);
            default -> {
                sender.sendMessage("§cUnknown subcommand. Use /nexus join|leave|status|admin");
                yield true;
            }
        };
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can join queues.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nexus join <gameId>");
            return true;
        }
        String gameId = args[1];
        queueManager.enqueue(player.getUniqueId(), player.getName(), gameId, 0);
        player.sendMessage("§aJoined queue for §6" + gameId + "§a.");
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can leave queues.");
            return true;
        }
        queueManager.remove(player.getUniqueId());
        player.sendMessage("§aLeft your current queue.");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        List<Minion> minions = minionRegistry.getAll();
        List<Turret> turrets = turretRegistry.getAll();
        long online = turrets.stream().filter(t -> t.getStatus() == TurretStatus.ONLINE).count();
        sender.sendMessage("§6Nexus Status");
        sender.sendMessage("§7Turrets online: §f" + online + "/" + turrets.size());
        sender.sendMessage("§7Live minions: §f" + minions.size());
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexus.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nexus admin list|turrets|drain|undrain|kill");
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "list" -> {
                List<Minion> minions = minionRegistry.getAll();
                if (minions.isEmpty()) {
                    sender.sendMessage("§7No live minions.");
                } else {
                    for (Minion m : minions) {
                        sender.sendMessage("§6" + m.getMinionId() + " §7[" + m.getState() + "] "
                                + "turret=" + m.getTurretId() + " players=" + m.getPlayerCount());
                    }
                }
                yield true;
            }
            case "turrets" -> {
                List<Turret> turrets = turretRegistry.getAll();
                if (turrets.isEmpty()) {
                    sender.sendMessage("§7No turrets registered.");
                } else {
                    for (Turret t : turrets) {
                        sender.sendMessage("§6" + t.getTurretId() + " §7[" + t.getStatus() + "] "
                                + t.getCurrentMinions() + "/" + t.getMaxMinions() + " minions, "
                                + t.getUsedRamMb() + "/" + t.getTotalRamMb() + " MB RAM");
                    }
                }
                yield true;
            }
            case "drain" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /nexus admin drain <turretId>"); yield true; }
                turretRegistry.setStatus(args[2], TurretStatus.DRAINING);
                sender.sendMessage("§aTurret §6" + args[2] + "§a set to DRAINING.");
                yield true;
            }
            case "undrain" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /nexus admin undrain <turretId>"); yield true; }
                turretRegistry.setStatus(args[2], TurretStatus.ONLINE);
                sender.sendMessage("§aTurret §6" + args[2] + "§a set to ONLINE.");
                yield true;
            }
            case "kill" -> {
                if (args.length < 3) { sender.sendMessage("§cUsage: /nexus admin kill <minionId>"); yield true; }
                Minion m = minionRegistry.get(args[2]);
                if (m == null) {
                    sender.sendMessage("§cMinion §6" + args[2] + "§c not found.");
                } else {
                    minionManager.onGameEnd(args[2], new dev.nexus.api.game.GameEndResult(args[2], null, null, null));
                    sender.sendMessage("§aForce-killed minion §6" + args[2] + "§a.");
                }
                yield true;
            }
            default -> {
                sender.sendMessage("§cUnknown admin subcommand.");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("join", "leave", "status", "admin");
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("list", "turrets", "drain", "undrain", "kill");
        }
        return new ArrayList<>();
    }
}
