# ⏳ Timerald — Playtime Economy Plugin for Minecraft

**Timerald** introduces a unique economy system based on player **playtime**. Players can convert their Emerald into a virtual currency called **Timerald**, spend it on useful items or playtime extensions, and even trade it with others.

---

## 🛠️ Features

### 🪙 Currency Management
- **Deposit System**: Convert emeralds or emerald blocks into Timerald  
- **Withdraw System**: Convert Timerald back into emeralds or blocks  
- **Player-to-Player Transfers**: Send Timerald to other players  
- **Cross-Platform Support**: Supports both **Java** and **Bedrock** players seamlessly  

### 🛍️ Playtime Shop
Purchase additional playtime or special items using Timerald:

- **Playtime Increments**:
  - ⏱️ 1 minute — 4 Timerald
  - ⏱️ 5 minutes — 20 Timerald
  - ⏱️ 10 minutes — 40 Timerald
  - ⏱️ 20 minutes — 64 Timerald

- **⏳ Time Elixir** (Special Consumable):
  - **Cost**: 64 Timerald  
  - **Effects on use**:
    - ⬇️ Reduces daily playtime limit by **20 minutes**
    - ❤️ Restores **3 hearts**
    - 🍗 Restores **3 hunger bars**
    - ✨ Grants **saturation**

---

## 📜 Commands

| Command | Description |
|--------|-------------|
| `/timerald` | Opens the Timerald shop GUI |
| `/timerald send <player> <amount>` | Sends Timerald to another player |
| `/deposit` | Converts emerald(s) in hand to Timerald |
| `/withdraw <amount>` | Converts Timerald back into emerald(s) |

---

## 🔧 Configuration

The plugin is fully configurable via `config.yml`, including:
- Shop prices and available increments
- Time Elixir cost, effects, and custom item name
- Limits and multipliers

---

## 📦 Dependencies

- ✅ **[PlayTimeLimiter](https://github.com/Pemrograman-Fasilkom-Unej/MC-PlayTimeLimiter)** — Required for managing daily playtime  
- 🎮 Minecraft version: **1.20+**

---

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `timerald.use` | Access to all Timerald commands | ✅ true |

---

## 🧪 Installation

1. Make sure **PlayTimeLimiter** is installed and running  
2. Place `Timerald.jar` in your server's `plugins` folder  
3. Restart your server  
4. Configure settings in `config.yml` (optional)

---

## 📣 Support & Suggestions

For issues, suggestions, or feature requests, please open an issue or reach out via our Discord server or plugin page.
