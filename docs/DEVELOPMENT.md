# Skilling Cost Calculator

RuneLite external plugin that estimates GP cost/profit for buyable or semi-buyable skilling
methods using live OSRS Wiki GE prices.

## Features

- Skill selection with automatic current-XP loading from the client (manual entry also works)
- Target level/XP fields that stay in sync; target defaults to one level above current
- Method list with lock indicators, a "hide locked methods" filter, and sorting by least
  actions (default) or most profitable; profit sorting also estimates every variant so the
  variant dropdown shows per-bone/per-variant net gp
- **Method groups**: related methods (e.g. every bone at a gilded altar, buy-vs-smelt bars,
  cleaning herbs) appear as a single row with a variant dropdown instead of dozens of entries
- Optional consumables toggle (thread, binding necklace, ...), sell-outputs toggle, GE tax toggle
- Full cost breakdown per method: input cost, output revenue, GE tax, net gp

## Method data

All methods live in [`src/main/resources/methods.json`](src/main/resources/methods.json) —
**no code changes are needed to add or fix a method.** Each entry lists the skill, name, level,
XP per action, inputs, optional inputs, and outputs (item ids + quantities). Entries that share
a `group` render as one row whose `variant` values populate the dropdown.

Prayer is generated from a table of bones instead of listing every combination: each bone has a
bury XP and a blessed-bone-shard count, and `MethodRepository` expands the table into all six
offering methods (bury, gilded altar, chaos altar, Ectofuntus, libation bowl with blessed or
sunfire wine). **Adding one bone line to the JSON adds it to all six groups.**

After editing the JSON, validate ids and names against the wiki:

```
python3 tools/validate_methods.py
```

### Method counts

| Skill | Methods (list rows) |
|---|---:|
| Smithing | 35 (21) |
| Herblore | 43 (26) |
| Fishing | 12 (12) |
| Cooking | 21 (21) |
| Prayer | 64 (7) |
| Crafting | 52 (52) |
| Firemaking | 9 (9) |
| Magic | 16 (16) |
| Fletching | 35 (35) |
| Runecraft | 14 (14) |
| Construction | 9 (9) |
| **Total** | **310 (222)** |

## Code layout

| Class | Responsibility |
|---|---|
| `SkillingCostPlugin` | Lifecycle, client XP reads, async orchestration, stale-response guards |
| `SkillingCostPanel` | Side panel UI: inputs, method list, variant dropdown, options |
| `ResultRenderer` | Renders status messages and cost breakdowns in the result area |
| `MethodRepository` | Loads `methods.json`, expands the Prayer bone table into method groups |
| `MethodListEntry` | One list row: a standalone method or a group of variants |
| `CostCalculator` | Pure cost/tax math (no I/O, no Swing) |
| `PriceService` | OSRS Wiki price API client with a 60s cache |
| `SkillingMethod` / `ItemQuantity` / `CostResult` / `WikiPrice` | Data model |
| `Formats` | gp/skill-name/item-line text formatting |

## Assumptions and known gaps

- Farming is excluded; Woodcutting/Mining/etc. have no buyable methods.
- Magic assumes no staff substitutions, so elemental rune costs may be overstated.
- Runecraft multiple-rune output at higher levels is not modeled.
- Fishing treats one action as one catch; catch rates and mixed spots are not modeled.
- Gilded altar assumes both burners lit (x3.5); marble/mahogany burner costs are ignored.
- Chaos altar uses the long-run average (x7 per bone consumed, from the 50% save chance).
- Ectofuntus (x4) counts one bucket of slime per bone, but buckets of slime are not GE
  tradeable, so they price at 0 gp (the quantity needed is still shown). Returned empty
  pots/buckets, ecto-tokens, and grinding trips are not modeled.
- Libation bowl methods include Virilis's 10 gp un-noting fee and fractional wine/splinter
  usage (rounded up over the whole calculation); Zealot robe shard saves are not modeled.
- Cooking assumes no burns; jug of wine assumes no bad wine (guaranteed at 68+).
- Missing/non-GE prices are treated as 0 gp rather than blocking the calculation.
- Calculations and profit sorting are asynchronous; stale responses are dropped after
  skill/method/input changes.

## Running locally

1. Open this folder in IntelliJ.
2. Use Gradle JVM 17.
3. Run the Gradle `run` task.
4. Enable Wiki prices in plugin config.
5. Use manual XP fields if the dev RuneLite client is not logged in.
