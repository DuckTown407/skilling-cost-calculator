# Development

Build with a Gradle JVM 17; production bytecode targets Java 11. Existing plugin
identity and every config group/key are preserved.

## Architecture

| Class | Responsibility |
|---|---|
| SkillingCostPlugin | Client-thread XP reads, background catalog loading/calculation, EDT lifecycle ownership |
| SkillingCostPanel / CalculatorSession | Session goals/preferences, editable fields, search/selection and one revision guard |
| CalculationRequest / CalculatorController | Immutable work capture and testable asynchronous boundary |
| EstimateScheduler | Disposable 250 ms Swing timer; deterministic fake in tests |
| MethodListEntry | Groups, eligible variants and remembered selection |
| ResultRenderer | Main totals, expandable exact resources/assumptions and separate status |
| ScrollablePanel | Tracks viewport width and keeps horizontal focus scrolling from cropping sidebar content |
| CostCalculator / CostResult | One pure, checked calculation and typed availability state |
| PriceService / PriceSnapshot / WikiPrice | Shared OkHttp request, 60-second immutable cache and actual-side quote metadata |
| MethodRepository / ResourceRegistry | Validated bundled recipes, Prayer expansion and compile-time gameval identities |

Catalog parsing and calculations run off the client thread. Network work uses
injected OkHttp `enqueue()`; Gson is injected. UI changes run on the Swing EDT.
Shutdown cancels acquisition and stops the panel timer without waiting for workers.
Call identity/generation, plugin lifecycle and panel/XP-edit revisions reject
obsolete responses.

## Catalog maintenance

Recipes are in `src/main/resources/methods.json`. Existing registry resources need
only data edits; a new resource requires a compile-time gameval `ItemID` entry.
See [the schema](METHOD_SCHEMA.md) for pricing modes, decimal quantities, batch
capacities, stable keys and visible assumptions.

The current catalog has **796 explicit recipes + 113 generated Prayer variants =
909 methods in 560 unfiltered rows across 11 skills**. Counts, source references,
shipped families and concrete deferrals are in [the coverage matrix](CATALOG_COVERAGE.md).
Adding an eligible bone with verified XP, shard yield and requirement expands it
into six offerings; do not add a bone without checking eligibility for each one.

## Automated verification

```powershell
./gradlew clean test
python -B -m unittest discover -s tools -p "test_*.py"
python -B tools/validate_methods.py
```

`--offline` on the validator checks structure and registered references without
network access. Live validation checks canonical GE identities and explicit non-GE
resources against the Wiki mapping. It does not establish recipe mechanics;
`CatalogMechanicsTest` contains source-backed dose/quantity/XP fixtures.

Other tests cover large and invalid quotes, exact rounding and overflow, tax
floor/cap/exemption, acquisition/cache/disable/re-enable behavior, delayed XP,
coalescing, selection/filtering, every option combination and disposed panels.
Tests use fakes and isolated Swing components, never RuneScape input.
`PanelLayoutTest` uses RuneLite's theme and fonts to lay out the full 242-pixel
sidebar at short and tall heights, including maximum XP, all skills, focus
scrolling, wrapped method rows and expanded results.

## Running locally

Run `./gradlew run` from the plugin root, or use the Gradle run task in IntelliJ
with JVM 17. For a Jagex Account, follow RuneLite's
[Using Jagex Accounts instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts)
to log in to the development client. Wiki prices default to enabled when no
preference is saved; existing saved choices take priority. Manual XP works while
logged out.

## Manual acceptance checklist

The developer verification below handles the technical cases. The user only needs
to check the running client during normal use:

1. Open the calculator. Confirm the text and buttons are fully visible and the
   spacing feels comfortable at your usual window size.
2. Choose a skill and click **Load current XP**. Confirm the level/XP matches your
   character. Set a higher target and select a familiar method; an estimate should
   appear automatically with Wiki prices enabled.
3. Change a checkbox, switch methods or skills, and return. Confirm it keeps your
   choice and remains clickable.

Report anything that looks wrong in plain language. Only the user can establish
in-game acceptance; automated checks and a successful JVM start cannot establish
the final layout on the user's display or verify their character's XP.

## Developer checklist coverage

Run the automated verification commands above. These checks use isolated Java and
Swing components, without creating or operating a RuneScape client.

To manually verify a Wiki-price default change, use a profile without a saved
preference and confirm **Enable Wiki prices** starts checked. An explicitly saved
disabled preference should stay off.

| Behavior | Verification |
|---|---|
| Large GE prices and exact/compact amounts | `WikiPriceTest` covers prices above the old integer limit, trillion-GP values, invalid sides and signed-long formatting. |
| Sidebar clipping, wrapping and focus scrolling | `PanelLayoutTest` uses RuneLite's theme/fonts across every skill and three sidebar heights, including expanded results and maximum XP. |
| Targets, searches, method variants and global choices | `CalculatorFlowTest` checks skill changes, sorting, explicit variants, XP reloads and all eight option combinations, including empty selections and tax while sales are off. |
| Rapid edits, invalid text and delayed responses | `CalculatorFlowTest` checks coalescing, XP edit guards, outdated successes/failures and disposed panels. |
| Search, locked methods and comparison totals | `CalculatorFlowTest` checks filtering, empty results and consistency between selected comparisons and breakdowns. |
| Recipe identities, doses, fees, batches and yields | `CatalogMechanicsTest` checks specific corrected and new mechanics. `MethodRepositoryTest` and the live validator check the complete catalog's structure, references and GE identities. Canonical identity validation alone does not prove recipe mechanics. |
| Missing quotes, untradeable supplies, fixed fees and quote warnings | `CostCalculatorTest` and `ResultRendererTest` check availability, excluded outputs, fallback, stale/unknown age, tax and rounding boundaries. |
| Refresh failures and recovery | `PriceServiceTest` simulates HTTP, network and malformed-response failures; `CalculatorFlowTest` checks the previous-snapshot notice and unchanged fetch time. |
| Disabled prices, cancellation and obsolete callbacks | `PriceServiceTest` checks disabling/re-enabling, request cancellation and zero-work offline estimates. Panel disposal is checked by `CalculatorFlowTest`; plugin lifecycle ownership is reviewed in `SkillingCostPlugin`. |

Record the user's result under OpenSpec task 6.3, fix any reported issue and repeat
its relevant checks. Archive only after acceptance and an archive request.
