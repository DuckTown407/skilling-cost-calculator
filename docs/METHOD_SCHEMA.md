# Method schema and calculations

Recipes are bundled in `src/main/resources/methods.json`; no user recipe files or
saved configuration keys are migrated. Each recipe has a stable `key`, supported
`skill`, display `name`, level from 1 through 99, positive finite `xp`, an `action`
unit, resource lists, and source/assumption notes. `group` and `variant` retain
related production choices. New recipes using existing resources are data edits.
A new resource requires an explicit entry in `ResourceRegistry` backed by a gameval
`ItemID` constant; reflection and raw numeric references are not used.

Each resource specifies `name`, `qty`, and `pricing`: `GE` requires a registered
`resource`; `UNTRADEABLE` requires a registered resource and collection assumptions;
`FIXED_GP` requires nonnegative integral `fixedPrice` and has no resource lookup.
An absent quote never reclassifies a GE item as untradeable. Include all required
consumables and guarantee costs. Optional costs cannot imply a success guarantee.

Quantities use decimal arithmetic. Inputs round up over the whole estimate;
expected fractional outputs round down. `unitsPerItem` defaults to one and can
describe charges (for example one necklace per 16 uses). `perBatch: true` consumes
uses per processing batch, including a final partial batch. Combination recipes
expose essence per batch; the initial batch size is 28 (editable from 1 to 1,000) and must be adjusted to the
player's actual inventory/pouches. Equipment, spellbooks, quests, collected supplies,
success assumptions, and incidental costs belong in visible `assumptions`.

`outputLevels` lists increasing levels at which the output multiplier increases by
one. Starting-level yield stays fixed for the entire estimate; this is not a route
optimizer. XP per action stays unchanged. Zero or already-met targets require no
actions, resources, or quotes.

One pure result supplies both comparisons and breakdowns. Missing included quotes
make a numeric estimate unavailable; missing excluded outputs do not. Untradeable
outputs create no sale revenue. Fixed fees contribute exact GP. GP operations and
quantity conversions are checked against the signed 64-bit range; excessive totals
are out of range, not wrapped or saturated.

GE tax floors the configured rate per item, applies the exemption threshold and
per-item cap, and then multiplies by quantity. Defaults are 200 basis points, a 50 GP
threshold, and a 5,000,000 GP cap. Verified exempt outputs can set `taxExempt: true`.
Invalid tax settings produce a readable validation state. Every changed recipe
needs source-backed numeric fixtures: item-name validation alone cannot verify XP,
dose, charges, success rates, or missing inputs.

Prayer's table expands each verified bone across six offering methods. Slime remains
explicitly untradeable; libation unnoting fees remain fixed GP; wine and splinters
round over the entire plan. Offering multipliers and average bone saves are visible.
