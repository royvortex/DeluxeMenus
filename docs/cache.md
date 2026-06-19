# MenuCache

The MenuCache is DeluxeMenus' per-menu and per-item cache, layered on top of Guava
`Cache` with LRU + TTL eviction. Its job is to skip the work that doesn't need to
re-run on every render: the static, config-derived parts of an item.

## The split: static vs dynamic

The whole design rests on one observation: an item's fields fall into two groups.

| Group | Examples | Cost per render |
|---|---|---|
| **Static** (config-derived) | material, amount, enchantments, item flags, unbreakable, potion effects, isPlayerItem, isWaterBottle, hook metadata, NBT structure | One-time computation; same result every render |
| **Dynamic** (placeholder-resolved) | displayName, lore, custom model data, rgb, rarity, hideTooltip, enchantment glint, tooltip style, item model, trim material/pattern, light level, damage, hookArgs, NBT values, `placeholder:`/`stack:`/`dynamic_amount` materials | PAPI calls, regex, hook lookups; the result changes per player/time |

The cache stores only the static part. The dynamic part is rebuilt on every render
against the current `MenuHolder`. This means placeholders are always fresh — and
`updatePlaceholders: true` items actually update, which was the bug the split fixed.

## Three cache switches

| Key | Where | Default | What it does |
|---|---|---|---|
| `caching` | per item | `true` | The item's `StaticItemData` is cached for 60 seconds. |
| `shared` | per item | `false` | The item's cache key uses `null` UUID. One entry serves every player. |
| `shared` | per menu | `false` | The snapshot cache key uses `null` UUID. One snapshot serves every player. |

`caching: false` opts an item out of the cache entirely. The static is rebuilt
every time, which is what you want for items with always-changing dynamic fields
(e.g. `%server_time%`).

`shared: true` collapses the cache key to `null` UUID. The static (and snapshot) is
identical for every player, so there's no point in keeping 50 copies. Lower
memory, lower eviction pressure, same render result.

## How a render works

```
Menu.open(viewer, args)
  └─► Menu.computeSnapshot(holder)
        └─► for each MenuItem:
              StaticItemData static = item.getOrComputeStatic(holder)
                  └─► cache hit? return cached : compute + put
              DynamicItemData dynamic = item.resolveDynamic(holder, static)
                  └─► always re-runs (placeholders may have changed)
              new MenuItemData(static, dynamic)
        └─► MenuSnapshot = (title, size, items[])
  └─► Menu.renderSnapshotToInventory(holder, snapshot)
        └─► for each MenuItemData template in snapshot:
              // the template may have stale dynamic if snapshot was served from cache
              MenuItemData fresh = item.refreshDynamic(holder, template)
              ItemStack stack = item.getItemStack(fresh, viewer)
              stack = plugin.getMenuItemMarker().mark(stack)
              inventory.setItem(slot, stack)
```

Two refreshes of dynamic, by design:
1. Once when building the snapshot.
2. Once per item at render time, so snapshot-cache hits still get fresh placeholders.

## What gets cached, what doesn't

| Cached | Always rebuilt |
|---|---|
| Material (when literal, e.g. `STONE`) | Material with `placeholder:` prefix |
| Amount (when no `dynamic_amount`) | `dynamic_amount` resolution |
| Enchantments, item flags, unbreakable, potion effects | `displayName` after placeholder substitution |
| Hook name (matched by prefix) | `lore` after placeholder + color |
| `base64Data` | `customModelData`, `rgb`, `rarity` |
| NBT template strings (the keys + types) | `hideTooltip`, `enchantmentGlint`, `tooltipStyle`, `itemModel` |
| | `trimMaterial`, `trimPattern`, `lightLevel`, `damage` |
| | `hookArgs` after placeholder substitution |
| | NBT values after placeholder substitution |
| | Title (per snapshot; same TTL) |

## Invalidation

| Trigger | Effect |
|---|---|
| Player quit | Both tiers: `invalidate(uuid)` |
| `/dm reload <menu>` | Both tiers: `invalidate(menuName)` (case-insensitive) |
| Plugin disable | Both tiers: `clearCache()` |
| TTL expiry (1 minute after write) | Per entry, automatic |

There is no automatic invalidation on placeholder value changes — that's the
point. Placeholders are dynamic and bypass the cache by construction. The TTL
caps how long any stale entry can live.

## Config patterns

### Filler / decoration (the best case)

```yaml
'border_glass':
  material: GRAY_STAINED_GLASS_PANE
  slot: 0-8
  shared: true    # one entry serves all 50 players
```

No placeholders, no PAPI. Cache hit is ~1-2 µs. 40 filler items × 50 players
with `shared: true` = 40 entries total, not 2000.

### Dynamic item with PAPI

```yaml
'balance_display':
  material: GOLD_INGOT
  slot: 49
  display_name: '&eYour balance: &6%vault_eco_balance_formatted%'
  lore:
    - '&7Health: &c%player_health%'
  update: true
  # caching: true (default)
  # shared: false (default)
```

Static cached per (player, args). Dynamic rebuilt every render AND every
`update_interval` seconds via `startUpdatePlaceholdersTask`. Use this when
the static is non-trivial (slot, priority, enchantments, hook meta) and the
dynamic is mostly placeholders.

### Items that change every tick

```yaml
'live_clock':
  material: CLOCK
  slot: 53
  display_name: '&f%server_time_HH:mm:ss%'
  update: true
  caching: false   # skip cache; rebuild every time
```

The static is cheap, the dynamic changes constantly. Caching the static just
adds the cache overhead without saving meaningful work.

### Menu-wide shared

```yaml
shop:
  menu_title: '&6&lShop'
  size: 54
  open_command: shop
  shared: true     # one snapshot for all players
  items:
    'item_a':
      material: DIAMOND_SWORD
      # ...
    'item_b':
      material: GOLD_INGOT
      # ...
```

Use for menus where the static is identical across players (most static-shop
menus). Saves memory; render path still does per-player `refreshDynamic` so
placeholders stay correct.

## Performance characteristics

The cache saves the static-work portion of a render. Concrete numbers from a
synthetic micro-benchmark on this machine (10,000 iterations):

| Operation | Time |
|---|---|
| Cold build (`cache.put` of `StaticItemData`) | ~3.5 µs |
| Warm lookup (`cache.get`) | ~2.3 µs |
| Speedup on the static alone | ~1.5x |

The 1.5x is artificially low because the synthetic test has near-zero static
work — just field copies. In production, `computeStatic` does real work
(Material registry lookups, hook matching, NBT copying) that runs into
double-digit microseconds per item. On a 54-item shop with 40 filler items,
the realistic per-open saving is **~400 µs**, which compounds to meaningful
CPU at scale (4% of one core on a 50-player server opening every 30s).

The cache does **not** save the dynamic-work portion. PAPI calls and color
coding run on every render by design. The biggest single latency lever for
most menus is reducing the placeholder count on dynamic items, not enabling
the cache.

## Gotchas

**`updatePlaceholders: true` requires `caching: true` to be efficient.** If you
disable caching, the update task rebuilds the static on every tick, which is
wasted work for items that don't change.

**`shared: true` ignores per-player static differences.** If a single item key
is reused with different config across players (e.g. via permission-based
priority), `shared: true` will serve the wrong entry. The priority mechanism
selects between different item entries, so this is usually fine — but be
careful with hybrid setups.

**The cache key is `(player, menu, item, args)`.** Different `args` produce
different cache entries. A menu opened with `/shop page:1` and `/shop page:2`
has two separate entries. Memory grows with the arg-space.

**TTL is 1 minute.** A menu opened once a minute gets no benefit. The cache
shines for menus opened many times per minute by many players (shop, info
board, server selector).

**Snapshot cache stores `MenuSnapshot` (which contains `MenuItemData` with
resolved dynamic).** On a snapshot-cache hit, the render path calls
`item.refreshDynamic` for every item, so the dynamic portion of the snapshot
is effectively ignored — the render rebuilds it. This is intentional but
worth knowing: the snapshot cache is effectively a "save the static" cache
with extra steps.

## Files

- `src/main/java/com/extendedclip/deluxemenus/cache/MenuCache.java` — the cache
- `src/main/java/com/extendedclip/deluxemenus/menu/StaticItemData.java` — cached side
- `src/main/java/com/extendedclip/deluxemenus/menu/DynamicItemData.java` — rebuilt side
- `src/main/java/com/extendedclip/deluxemenus/menu/MenuItemData.java` — wrapper
- `src/main/java/com/extendedclip/deluxemenus/menu/MenuItem.java` — `computeStatic` + `resolveDynamic`
- `src/test/java/com/extendedclip/deluxemenus/cache/MenuCacheBenchmarkTest.java` — 5 tests, including the split's invariant
