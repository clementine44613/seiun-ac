# 🛡️ Seiun AC — Minecraft Anti-Cheat Mod

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b47a?logo=minecraft&logoColor=white)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-%3E%3D0.18.5-2e3440?logo=fabric&logoColor=white)
![Java](https://img.shields.io/badge/Java-21+-orange?logo=openjdk&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0.5-blue)
![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-lightgrey)

A Fabric anti-cheat mod that **verifies client mods and resource packs** against a server-side whitelist, blocking cheaters and alerting admins via Discord.

Companion project to [**seiun**](https://github.com/clementine44613/seiun) — while `seiun` handles Discord-based whitelisting, `seiun-ac` enforces client integrity in-game.

Designed for **Minecraft 1.21.11** with **Fabric Loader 0.18.5+** and **Java 21**.

---

## Features

- **Mod verification** — checks every client mod against a server-side hash whitelist
- **Resource pack verification** — detects unauthorized or modified resource packs
- **Gray list support** — allows trusted but unverified mods/packs that trigger warnings instead of kicks
- **Discord webhooks** — real-time alerts for kicks, warnings, pack changes, and server status
- **Op tracking** — logs when players gain or lose operator status
- **Pack change detection** — monitors resource pack changes mid-session
- **Statistics** — per-player and global violation tracking
- **Configurable** — whitelist, blacklist, gray lists, and Discord settings via JSON files

---

## How it works

1. A player joins the server.
2. The server sends a verification request to the client.
3. The client responds with its installed mod list and hashes.
4. The server compares each mod against:
   - **Whitelist** — approved mods, no action
   - **Gray list** — unknown mods, warning only
   - **Blacklist** — prohibited mods, kick
5. If violations are found, the player is kicked with a detailed reason.
6. If only warnings are found, the player is allowed to join but receives an in-game warning and a Discord notification.

---

## Prerequisites

- **Minecraft 1.21.11** server with **Fabric Loader 0.18.5+**
- **Fabric API** installed
- **Java 21**
- A **Discord bot / webhook** (optional, for notifications)

---

## 1. Install the mod

Download the latest JAR from [Modrinth](https://modrinth.com/mod/seiunac) and drop it into your server's `mods` folder.

The mod creates its config directory automatically on first run:

```
config/SeiunAC-anticheat/
├── verification/
│   ├── config.json
│   ├── mod_whitelist.json
│   ├── mod_blacklist.json
│   ├── pack_whitelist.json
│   ├── pack_blacklist.json
│   ├── graymods/
│   └── graypacks/
└── discord/
    └── webhook.json
```

---

## 2. Set up the mod whitelist

Edit `config/SeiunAC-anticheat/verification/mod_whitelist.json` to approve mods:

```json
{
  "whitelist": [
    "fabric-api",
    "minecraft"
  ]
}
```

---

## 3. (Optional) Configure Discord webhooks

Edit `config/SeiunAC-anticheat/discord/webhook.json`:

```json
{
  "enabled": true,
  "webhooks": {
    "playerJoin": "https://discord.com/api/webhooks/...",
    "playerKick": "https://discord.com/api/webhooks/...",
    "modsWarning": "https://discord.com/api/webhooks/...",
    "packsWarning": "https://discord.com/api/webhooks/..."
  },
  "features": {
    "playerJoin": true,
    "opJoin": true,
    "playerKick": true,
    "modsWarning": true,
    "packsWarning": true,
    "packChangeLog": true
  }
}
```

---

## Commands

| Command | Description |
| --- | --- |
| `/seiunac reload` | Reload verification lists and config |
| `/seiunac status` | Show mod verification status |
| `/seiunac stats` | Show violation statistics |
| `/seiunac whitelist list` | List whitelisted mods |
| `/seiunac whitelist add <mod>` | Add a mod to the whitelist |
| `/seiunac whitelist remove <mod>` | Remove a mod from the whitelist |

---

## Troubleshooting

- **"Anti-cheat mod not found"** — The client doesn't have Seiun AC installed. They need to install it from [Modrinth](https://modrinth.com/mod/seiunac).
- **"Anti-cheat mod has been modified"** — The client's Seiun AC JAR hash doesn't match the server's expected hash. They may be using a cracked or altered version.
- **"Illegal mods detected"** — The client has a mod on the blacklist. Check the kick message for the mod name.
- **"Modified mods detected"** — A whitelisted mod has a different hash on the client than on the server. The mod file may have been modified.
- **Slash commands not appearing** — Make sure you have the latest Fabric API installed and restart the server.

---

## Related projects

- [**seiun**](https://github.com/clementine44613/seiun) — Discord bot for auto-whitelisting and verifying Minecraft usernames via SFTP

## License

All rights reserved. See the mod page on [Modrinth](https://modrinth.com/mod/seiunac) for distribution terms.
