# Nexus — Local Test Environment

This folder contains a Docker Compose setup for running a full Nexus test environment locally.
It spins up Redis, a Velocity proxy, a hub server, and one turret server — everything you need
to test Nexus without any external hosting.

---

## Requirements

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- A Minecraft Java Edition client (1.21.11)

---

## First-Time Setup

### 1. Start the environment

Open a terminal in this folder and run:

```bash
docker compose up -d
```

Docker will download the images and create the `velocity/`, `hub/`, and `turret-1/` folders
automatically. Wait about a minute for all servers to fully boot.

---

### 2. Configure Velocity forwarding

Open `velocity/velocity.toml` and make the following changes:

**Set the forwarding mode to MODERN:**
```toml
player-info-forwarding-mode = "MODERN"
```

**Add your backend servers under `[servers]`:**
```toml
[servers]
hub = "nexus-hub:25566"
turret-1 = "nexus-turret-1:25567"

try = ["hub"]
```

---

### 3. Copy the forwarding secret

Open `velocity/forwarding.secret` and copy the contents (a random string).

You need to paste this into **both** the hub and turret-1 configs.

**hub** → `hub/config/paper-global.yml`:
```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "paste-your-secret-here"
```

**turret-1** → `turret-1/config/paper-global.yml`:
```yaml
proxies:
  velocity:
    enabled: true
    online-mode: true
    secret: "paste-your-secret-here"
```

---

### 4. Verify online-mode is disabled on backend servers

Both `hub/server.properties` and `turret-1/server.properties` must have:
```properties
online-mode=false
```

The proxy handles authentication — backend servers must be in offline mode.

---

### 5. Install plugins

Drop your built plugin JARs into the correct folders:

| Plugin | Folder |
|--------|--------|
| `nexus-hub.jar` | `hub/plugins/` |
| `nexus-velocity.jar` | `velocity/plugins/` |
| `revenant-plugin.jar` (when ready) | `turret-1/plugins/` |

---

### 6. Configure nexus-hub

After first boot, a config file will be generated at `hub/plugins/NexusHub/config.yml`.
Update the Redis host to use the container name:

```yaml
nexus:
  redis:
    host: "nexus-redis"
    port: 6379
    password: ""
    pool-size: 10

  turrets:
    - id: "turret-1"
      host: "nexus-turret-1"
      max-minions: 5
      total-ram-mb: 4096
      api-key: ""
      tags: ["all"]

  hub:
    return-server: "hub"
```

---

### 7. Configure nexus-velocity

After first boot, update `velocity/plugins/NexusVelocity/config.yml`:

```yaml
nexus:
  redis:
    host: "nexus-redis"
    port: 6379
    password: ""
  hub-servers:
    - "hub"
```

---

### 8. Restart everything

After all config changes are done:

```bash
docker compose restart
```

---

## Connecting

Open Minecraft and connect to `localhost`. Velocity will route you to the hub automatically.

To op yourself, open the hub console via Docker Desktop (Containers → nexus-hub → Exec tab):
```
rcon-cli op YourUsername
```

---

## Useful Commands

| Command | Description |
|---------|-------------|
| `docker compose up -d` | Start all containers |
| `docker compose down` | Stop all containers |
| `docker compose restart` | Restart all containers |
| `docker compose restart hub` | Restart only the hub |
| `docker compose logs -f hub` | Watch hub logs live |
| `docker compose logs -f velocity` | Watch Velocity logs live |

**Inspect Redis:**
```bash
docker exec -it nexus-redis redis-cli
KEYS *
HGETALL nexus:turret:turret-1
```

---

## Notes

- `forwarding.secret` is gitignored — never commit it to a public repository.
- Server data folders (`velocity/`, `hub/`, `turret-1/`) are gitignored — they contain generated
  world data, logs, and configs specific to your local machine.
- The `turret-1` container represents a local test turret. In production, turrets are separate
  physical or virtual machines registered in the nexus-hub config.
- Redis has no password in this local setup. Do not expose port 6379 to the public internet.
