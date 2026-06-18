# Skilling Cost Calculator

RuneLite external plugin starter that estimates GP cost/profit for buyable or semi-buyable skilling methods.

## Current scope

UI includes skill selection, manual/current XP, sorting, optional consumables, output sale toggle, GE tax toggle, and a **Hide locked methods** filter.


- Multi-skill method list
- Manual current/target XP fields
- Attempts to load selected skill XP from the RuneLite client when switching skills
- Scrollable method list
- Optional consumables toggle with names
- Sell output and GE tax toggles
- Sort by default, least actions, or most profitable
- Method rows show actions when action sorting is selected, and net GP when profit sorting is selected
- Larger method data pass across Smithing, Herblore, Fishing, Cooking, Prayer, Crafting, Firemaking, Magic, Fletching, Runecraft, and Construction
- Smithing now includes separate "buy bars" and "smelt bars" routes for bar-based methods such as platebodies, dart tips, and cannonballs

## Method counts

| Skill | Methods |
|---|---:|
| Smithing | 35 |
| Herblore | 25 |
| Fishing | 12 |
| Cooking | 20 |
| Prayer | 16 |
| Crafting | 51 |
| Firemaking | 9 |
| Magic | 10 |
| Fletching | 34 |
| Runecraft | 14 |
| Construction | 9 |
| **Total** | **235** |

## Notes

- This build includes a cleanup pass for stale async results, empty price lookups, scroll reset behavior, and User-Agent fallback handling.
- Farming is intentionally excluded.
- Magic currently assumes no staff substitutions, so elemental rune costs may be overstated.
- Runecraft multiple-rune output by level is not modeled yet.
- Fishing catch rates and mixed fishing spots are not modeled; each method is treated as one action = one listed catch.
- Smithing "smelt bars" methods treat the ores/coal as the purchased inputs and the final smithed item as the output. XP/action includes both smelting the bars and smithing the item.
- Missing/non-GE item prices are treated as 0 gp rather than blocking the calculation.
- Calculations and profit-sort requests are asynchronous; stale responses are ignored after skill/method/input changes.

## Running locally

1. Open this folder in IntelliJ.
2. Use Gradle JVM 17.
3. Run the Gradle `run` task.
4. Enable Wiki prices in plugin config.
5. Use manual XP fields if the dev RuneLite client is not logged in.
