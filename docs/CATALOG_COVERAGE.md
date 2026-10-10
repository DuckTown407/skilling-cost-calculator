# Catalog coverage

Reviewed on 2026-10-10. Scope stays at the existing 11 skills. The catalog contains
796 explicit recipes and 113 generated Prayer variants: **909 methods in 560
unfiltered rows**, compared with 310 methods before this change. Search and locked
method filtering can reduce those counts. Group rows rank their selected variant.

| Skill | Methods | Grouped list rows |
|---|---:|---:|
| Smithing | 321 | 170 |
| Herblore | 64 | 45 |
| Fishing | 19 | 19 |
| Cooking | 50 | 50 |
| Prayer | 136 | 9 |
| Crafting | 110 | 91 |
| Firemaking | 29 | 29 |
| Magic | 73 | 41 |
| Fletching | 72 | 72 |
| Runecraft | 24 | 23 |
| Construction | 11 | 11 |
| **Total** | **909** | **560** |

## Coverage decisions

The matrix records what shipped and what still needs a different model or further
recipe verification. It is not a claim that every activity in the game is modeled.

| Skill | Shipped or reconciled | Named deferrals and reasons |
|---|---|---|
| Smithing | Standard bronze through rune weapons, armour, limbs, nails and ammunition; bought-bar and ordinary-furnace combined variants; all six basic metal cannonballs and chainshot/incendiary upgrades. Required ring of forging guarantees ordinary iron smelting. | Blast Furnace fees depend on time and worker charges; Giants' Foundry depends on alloy, quality and completion stages. Barbarian spears/hastae, lead/cupronickel and Sailing boat components need separate verification of extra materials, unlocks and processing stages before they become fixed recipes. |
| Herblore | Huasca cleaning; goading/prayer regeneration from unfinished potions or clean herbs; stamina, extended/super antifire, antivenom/antivenom+, bastion/battlemage and eight divine potion variants. Correct three-dose outputs and four-dose Super combat precursors. | Mixology commissions combine multiple potion orders and variable rewards. Amulet of chemistry, goggles and other probabilistic dose/reagent bonuses need their own outcome model. |
| Fishing | Additional single-catch anchovies, mackerel, cod, pike, tuna, bass and rainbow fish; correct sandworms for anglerfish and dark fishing bait for dark crabs. Existing collected karambwanji remains explicit. | Mixed nets/harpoons, minnows, eels, Fishing Trawler and Tempoross need catch/conversion or reward distributions. Sailing shoals/routes need species mixtures, travel and boat supplies rather than a fixed single-catch estimate. |
| Cooking | Released fish including Sailing species; bread, eleven pies, plain pizza from a bought base and separate bought-pizza topping/full-from-base variants. Neypotzli bream uses collected supplies. | Burn rates, bad wine, variable double-cook timing and cooked-food bonuses are not simulated. No-burn and successful-wine assumptions remain visible. Brewing/cookout reward systems require fermentation/failure or activity-specific inputs. |
| Prayer | 18 verified bone entries across six offerings; five ash types including fiendish; Sinister and Demonic Offering consume three remains plus their runes. Correct ash identities and bone shard yields; explicit collected slime and fixed unnoting fees. | Wolf, jogre, burnt, monkey and shaikahan bones are not added to the shared table until eligibility and shard yields are verified for every generated offering. Ensouled-head reanimation adds combat/travel supplies and outcomes; random cremation rewards and Zealot saves are excluded. |
| Crafting | Four amethyst component batches; remaining gem jewellery in the reviewed gold/silver families; wool/flax spinning; bought-leather or tan-hide-and-craft variants with Ellis fees. Hardleather body now requires level 28. | Standalone tanning grants no Crafting XP, so it cannot advance a target. Sandstone grinding, drift-net gathering, shade keys, crystal/boat assemblies and charged equipment workflows need collection, multiple-skill or extra-stage verification. |
| Firemaking | Achey, arctic pine, jatoba, camphor, ironwood, rosewood and collected blisterwood logs; 13 pyre-log lighting variants using bought pyre logs and Loar remains. | Wintertodt and Forestry activities depend on level, points and rewards. Pyre-log preparation uses sacred-oil dose states and Prayer XP; this pass prices bought pyre logs instead. Random shade keys/rewards are not valued. |
| Magic | Seven Plank Make types and fees; eight standard Superheat bars; four orb spells; five-flax spinning, five-hide tanning and normal/giant seaweed glass batches; corrected String Jewellery and Stun supplies. Single-element staff variants make equipment assumptions explicit. | General alchemy revenue requires selecting the actual item and its alchemy value; the retained rune-cost-only recipe states that limitation. Tablet/lectern recipes, other jewellery enchants and multi-spell/equipment combinations require separate material/unlock verification. |
| Fletching | Four atlatl stages plus full production; amethyst ammunition; bronze through rune arrows/bolts/javelins, dragon components, six wooden shields and redwood hiking staff. Stringing maple longbows uses 58.2 XP, distinct from cutting. | Vale Totems combines offering tiers and random rewards. Specialized crossbow/boat assemblies, alternate shaft materials and further tipped-bolt chains need separate stage/yield verification; ordinary reviewed ammunition uses bought intermediates. |
| Runecraft | Astral, sunfire, wrath, aether and all six elemental combinations; pure-essence blood separated from collected dark-essence blood/soul; starting-level multiple-rune yields. Aether includes catalyst, soul rune, Magic Imbue and necklace charges. | Ourania/GotR reward mixtures, progression through yield thresholds, outfits, extracts, blood essence and daeyalt bonuses are not modeled. These require additional yield/input rules; standard recipes explicitly exclude them. |
| Construction | Mounted mythical capes, oak dining tables and existing furniture; consumed planks/nails and reusable cape/tools distinguished. | Mahogany Homes contracts vary by house and repair quantities; Sailing construction, other furniture and unlock-specific loops need their own resource/XP verification. Nail breaks, outfit bonuses and travel costs are excluded. |

## Sources and verification

Resource identity uses compile-time RuneLite gameval constants and the live
[OSRS Wiki mapping](https://prices.runescape.wiki/api/v1/osrs/mapping). Missing
mapping entries are investigated individually; they never automatically become
untradeable. The final canonical check covers 709 distinct resource/name
references, including nine intentionally non-GE identities.

Recipe levels/XP and material facts were checked against the following sources.
RuneLite tables sometimes quote XP per produced component; catalog action sizes
explicitly convert those values to the batch actually consumed.

- RuneLite skill calculator tables: [Smithing](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/SmithingAction.java),
  [Crafting](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/CraftingAction.java),
  [Fletching](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/FletchingAction.java),
  [Cooking](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/CookingAction.java),
  [Fishing](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/FishingAction.java),
  [Firemaking](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/FiremakingAction.java),
  [Magic](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/MagicAction.java).
- Jagex: [Final Dawn](https://secure.runescape.com/m=news/varlamore-the-final-dawn-out-now?oldschool=1)
  for atlatl stages and hiking staff;
  [The Red Reef release](https://secure.runescape.com/m=news/the-red-reef-is-out-today?oldschool=1)
  and [Rubium/Smithing changes](https://secure.runescape.com/m=news/rubium-mining--smithing-changes-?oldschool=1)
  for two-cannonball upgrade inputs and revised XP.
- Wiki mechanics: [Herblore](https://oldschool.runescape.wiki/w/Herblore),
  [Plank Make](https://oldschool.runescape.wiki/w/Plank_Make),
  [Combination runes](https://oldschool.runescape.wiki/w/Combination_rune),
  [Sunfire rune](https://oldschool.runescape.wiki/w/Sunfire_rune),
  [Air rune products](https://oldschool.runescape.wiki/w/Air_rune),
  [Water rune products](https://oldschool.runescape.wiki/w/Water_rune),
  [D'hide tanning](https://oldschool.runescape.wiki/w/Crafting_d%27hide),
  [Fishing calculator](https://oldschool.runescape.wiki/w/Module%3ASkill_calc/Fishing),
  [Bone shard table](https://oldschool.runescape.wiki/w/Module%3APrayer/Blessed_Bone_Shards)
  and [Offering spells](https://oldschool.runescape.wiki/w/Necromancy_spell).
- When direct Wiki retrieval was blocked, current copies supplied readable
  revision provenance for [aether](https://osrsindex.com/wiki/aether-rune?site=osrs_wiki),
  [bream](https://osrsindex.com/wiki/raw-bream?site=osrs_wiki),
  [jatoba logs](https://danielpgleason.com/osrs/reference/jatoba-logs-383373e7/)
  and [pizza ingredients](https://danielpgleason.com/osrs/reference/plain-pizza-b856e885/).
  The release/item IDs were also checked against RuneLite and the live GE mapping.

`CatalogMechanicsTest` locks down corrected doses/ashes, guarantees, batch units,
spell fees/runes, new families, starting yields and intermediate/full-production
differences. Other offline tests cover invalid catalog structure, exact quantities,
tax overflow, quote metadata, cache/cancellation and delayed UI state.

Run `./gradlew clean test`, `python -B -m unittest discover -s tools -p "test_*.py"`
and `python -B tools/validate_methods.py`. These checks do not establish in-game
acceptance; follow [the manual checklist](DEVELOPMENT.md#manual-acceptance-checklist).
