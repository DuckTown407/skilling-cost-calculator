# Price behavior

GP quotes and totals support signed 64-bit integers. The feed can contain prices above
2,147,483,647 GP, including unrelated items, without breaking other quotes. Positive
whole-number quotes are valid; missing, malformed, nonpositive, fractional, and
out-of-range values are unavailable rather than free. Unsupported training totals
produce an out-of-range estimate instead of wrapping.

Purchases use instant-buy quotes and sales use instant-sell quotes. When only the
opposite quote exists, the breakdown identifies the fallback. Quote age belongs to
the side actually used: quotes older than ten minutes are flagged, and absent or
invalid timestamps have unknown age. A fresh feed snapshot can contain old trades.

The summary uses compact GP labels, including trillions; the breakdown retains exact
whole GP. The plugin retains the OSRS Wiki v1 latest endpoint. Larger values do not
require an assumed API-version migration.

Wiki prices default to enabled when no preference is saved. The existing
configuration setting and third-party-server warning remain, and an explicitly
saved disabled preference takes priority. Users can turn prices off at any time.
Estimates with only fixed fees or untradeable resources need no network access.
Concurrent estimates share one asynchronous request. A successful snapshot is reused
for 60 seconds, including absent quotes and empty data. `Refresh prices` bypasses a
fresh cache, or joins an already running request. Snapshot age and trade age are
separate. A failed refresh retains the previous displayed estimate only with a
previous-snapshot notice; it does not make old data fresh. Disabling prices or the
plugin cancels acquisition and discards delayed responses without waiting.
