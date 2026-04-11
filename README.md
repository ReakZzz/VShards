# V-Shards


A lightweight, UUID-based virtual currency plugin with a GUI shop, leaderboard, PlaceholderAPI support, and a fully configurable MiniMessage interface.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/shards` | `vshards.balance` | Check your own shard balance |
| `/shards <player>` | `vshards.balance.others` | Check another player's balance |
| `/shards give <player> <amount>` | `vshards.admin.give` | Give shards to a player |
| `/shards take <player> <amount>` | `vshards.admin.take` | Remove shards from a player |
| `/shards set <player> <amount>` | `vshards.admin.set` | Set a player's exact balance |
| `/shards top [page]` | `vshards.top` | View the shard leaderboard |
| `/shards shop` | `vshards.shop` | Open the Shards Shop GUI |
| `/shards reload` | `vshards.admin.reload` | Reload all config files |

**Aliases:** `/vs`, `/vshards`

### Default Permissions

| Permission | Default |
|---|---|
| `vshards.balance` | Everyone |
| `vshards.top` | Everyone |
| `vshards.shop` | Everyone |
| `vshards.balance.others` | OP |
| `vshards.admin.*` | OP |

---

## PlaceholderAPI

| Placeholder | Returns |
|---|---|
| `%vshards_amount%` | The requesting player's shard balance |
| `%vshards_amount_<player>%` | A named player's shard balance |

Balances are formatted using the same K / M / B abbreviation system as in-game (e.g. `10K`, `2.5M`, `1B`).

---

## Number Formatting

All balance and price values across commands, messages, and placeholders are automatically abbreviated:

| Raw Value | Displayed As |
|---|---|
| 500 | `500` |
| 1,000 | `1K` |
| 10,000 | `10K` |
| 10,500 | `10.5K` |
| 1,000,000 | `1M` |
| 2,500,000 | `2.5M` |
| 1,000,000,000 | `1B` |

Whole numbers never show a decimal (e.g. `10K` not `10.0K`). Decimals only appear when meaningful (e.g. `10.5K`).

---

## Configuration

### config.yml

```yaml
shards_shop:
  enabled: true          # Enable or disable the shop GUI
  open-command: 'shop'   # Alias command to open shop ('' to disable)
  lock-gui: true         # Prevent players moving items inside the GUI

currency:
  starting-balance: 0    # Balance given to new players
  max-balance: -1        # Maximum allowed balance (-1 = unlimited)

storage:
  auto-save-interval: 300  # Seconds between auto-saves (default: 5 min)

leaderboard:
  entries-per-page: 10
```

All messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) formatting, including gradients and hex colors:

```yaml
messages:
  prefix: "<gray>[<gradient:#55FFFF:#5555FF>ᴠ-ꜱʜᴀʀᴅꜱ</gradient>]</gray> "
  balance-self: "<gray>ʏᴏᴜ ʜᴀᴠᴇ <gold>%amount% ꜱʜᴀʀᴅꜱ</gold>.</gray>"
  shop-buy-success: "<gray>ʏᴏᴜ ʙᴏᴜɢʜᴛ <white>%item%</white> ꜰᴏʀ <gold>%price% ꜱʜᴀʀᴅꜱ</gold>.</gray>"
```

#### Available message placeholders

| Placeholder | Used in |
|---|---|
| `%amount%` | Balance & admin messages |
| `%player%` | Balance, admin, error messages |
| `%sender%` | Give / take / set received messages |
| `%item%` | Shop buy / sell messages |
| `%price%` | Shop buy / sell messages |
| `%rank%` | Leaderboard entry |
| `%page%` / `%maxpage%` | Leaderboard header |
| `%usage%` | Error usage message |

---

### shop.yml

Defines the GUI layout and all shop items. Reloads with `/shards reload` — no restart needed.

```yaml
menu_title: '&dꜱʜᴀʀᴅꜱ ꜱʜᴏᴘ'
size: 54  # Multiple of 9, between 9 and 54

items:

  # ── Buy item ──────────────────────────────────────
  diamond_buy:
    material: DIAMOND
    slot: 13
    type: buy          # buy | sell | close | decoration
    price: 300
    amount: 1
    display_name: '&b&lᴅɪᴀᴍᴏɴᴅ'
    lore:
      - '&7ᴄᴏꜱᴛ: &b300 ꜱʜᴀʀᴅꜱ'
      - '&7ᴄʟɪᴄᴋ ᴛᴏ ʙᴜʏ!'

  # ── Sell item ─────────────────────────────────────
  diamond_sell:
    material: DIAMOND
    slot: 14
    type: sell
    price: 150
    amount: 1
    display_name: '&c&lꜱᴇʟʟ ᴅɪᴀᴍᴏɴᴅ'
    lore:
      - '&7ᴇᴀʀɴ: &6150 ꜱʜᴀʀᴅꜱ'
      - '&7ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟʟ!'

  # ── Command-based buy (no item given — command handles reward) ──
  vip_rank:
    material: NETHER_STAR
    slot: 16
    type: buy
    price: 50000
    amount: 0          # Set to 0 when using commands
    command-console:
      - 'lp user %player% parent set vip'
    command-player:
      - 'me just bought VIP!'
    display_name: '&6&lᴠɪᴘ ʀᴀɴᴋ'
    lore:
      - '&7ᴄᴏꜱᴛ: &650K ꜱʜᴀʀᴅꜱ'

  # ── Decoration (no interaction) ───────────────────
  border:
    material: GRAY_STAINED_GLASS_PANE
    slot: 0
    type: decoration
    display_name: '&r'
    lore: []

  # ── Close button ──────────────────────────────────
  close:
    material: RED_STAINED_GLASS_PANE
    slot: 49
    type: close
    display_name: '&c&lᴄʟᴏꜱᴇ'
    lore:
      - '&7ᴄʟɪᴄᴋ ᴛᴏ ᴄʟᴏꜱᴇ.'
```

#### Item field reference

| Field | Required | Notes |
|---|---|---|
| `material` | ✅ | Any valid Bukkit `Material` name (e.g. `DIAMOND`, `SPAWNER`) |
| `slot` | ✅ | Inventory slot index, `0` to `size - 1` |
| `type` | ✅ | `buy`, `sell`, `close`, or `decoration` |
| `price` | buy / sell | Shards deducted (buy) or rewarded (sell) |
| `amount` | buy / sell | Items given/taken. Use `0` alongside `command-console` / `command-player` |
| `command-console` | optional | List of commands run as console. Supports `%player%` |
| `command-player` | optional | List of commands run as the player. Supports `%player%` |
| `display_name` | ✅ | Supports `&` color codes |
| `lore` | optional | List of lore lines. Supports `&` color codes |

> **Tip:** Slots are 0-indexed. For a 54-slot inventory (6 rows), valid slots are `0`–`53`. Two items sharing the same slot will cause one to silently overwrite the other.

---

## Data Storage

Balances are stored in `plugins/V-Shards/data.yml`, keyed by **UUID**. This means balances survive player name changes.

Only modified balances are written on each auto-save cycle *(dirty-tracking)* — IO overhead stays minimal regardless of player count. A full save also runs automatically on server shutdown.

---

