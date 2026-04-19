# Nexus

**Nexus** is a gamemode-agnostic, horizontally scalable minigame instance management system
for Minecraft networks. It consists of three components:

- `nexus-api` shared interfaces and contracts (a library, not a plugin)
- `nexus-hub` a Paper plugin running on hub servers; handles queuing, matchmaking, and minion lifecycle
- `nexus-velocity` a Velocity plugin; handles player routing and dynamic server registration

All shared state (queues, minion registry, turret registry) lives in **Redis**, meaning any number
of hub servers can run in parallel without coordination problems.

When load increases, Nexus distributes new minions across all registered turrets by selecting the
least loaded one. Adding capacity is as simple as registering a new turret, no code changes needed.

## High-Level Architecture

```
   Players connect
          │
     [Velocity]  ←── load-balances across hub nodes
          │
     ┌────┴────┐
  [hub-1]   [hub-2]     ← both run nexus-hub, share all state via Redis
          │
       [Redis]          ← source of truth for queues, minions, turrets
          │
  TurretProvisioner
          │
  ┌───────┬───────┬─────┐
[T-1]   [T-2]   [T-3]   ← turrets (physical/virtual hosts)
  │       │       │
[min-1] [min-3] [min-5] ← minions (one running match each)
[min-2] [min-4]
```

When Nexus needs a new minion it asks the `TurretSelector`:
"Which turret has capacity?" The provisioner then spawns a new server process on that turret
and registers the resulting minion with Velocity.

## Core Concepts

### 1. NexusGame - The Gamemode Contract
Every minigame implements `NexusGame` and annotates itself with `@GameMeta`.
Nexus never touches game logic - it only calls lifecycle hooks.

```java
@GameMeta(
    id = "bedwars",
    displayName = "Bedwars",
    minPlayers = 8,
    maxPlayers = 8,
    instanceTimeoutSeconds = 720
)
public class BedwarsGame implements NexusGame {

    @Override
    public void onMinionStart(Minion minion, List<UUID> players) {
        // Called by Nexus when the minion is ready and players are assigned.
        // Set up the map, initialize loot/chests, start the countdown, etc.
    }

    @Override
    public void onMinionEnd(Minion minion, GameEndResult result) {
        // Called when the game ends. Nexus retires the minion after this returns.
    }

    @Override
    public boolean canJoinMidGame() {
        return false;
    }
}
```

### 2. GameMeta Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameMeta {
    String id();                          // Unique identifier, used as Redis key segment
    String displayName();                 // Human-readable name shown in GUIs
    int minPlayers();                     // Queue threshold to start a match
    int maxPlayers();                     // Hard cap per minion
    int instanceTimeoutSeconds();         // Force-kill minion after this many seconds
    String mapPool() default "default";   // Which map pool to draw from
    boolean allowTeams() default false;
    int teamSize() default 1;
    boolean reuseMinion() default false;  // If true, minion runs multiple matches before dying
    int minionRamMb() default 1024;       // RAM to allocate per minion (used by provisioner)
    int minionDiskMb() default 512;       // Disk space to allocate per minion (used by provisioner)
}
```