#!/usr/bin/env python3
"""Validates src/main/resources/methods.json against the OSRS Wiki item mapping.

Checks that every item id exists and that the name in methods.json matches the
canonical item name (so a typo'd id is caught immediately). Run it after adding
new methods:

    python3 tools/validate_methods.py

Requires network access to prices.runescape.wiki.
"""
import json
import sys
import urllib.request
from pathlib import Path

MAPPING_URL = "https://prices.runescape.wiki/api/v1/osrs/mapping"
USER_AGENT = "skilling-cost-calculator method validator"

# Items referenced by methods.json that are intentionally not GE tradeable
# (they price at 0 gp in the calculator). The mapping API won't know them.
KNOWN_NON_GE = {
    4286: "Bucket of slime",
}

REPO = Path(__file__).resolve().parent.parent
METHODS = REPO / "src" / "main" / "resources" / "methods.json"


def fetch_mapping():
    req = urllib.request.Request(MAPPING_URL, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return {item["id"]: item["name"] for item in json.load(resp)}


def iter_items(data):
    """Yields (context, id, name) for every item reference in methods.json."""
    for method in data["methods"]:
        for key in ("inputs", "optional", "outputs"):
            for item in method.get(key) or []:
                if item["id"] >= 0:  # negative ids are fixed-price gp fees
                    yield method["name"], item["id"], item["name"]
    prayer = data["prayer"]
    for key in ("jugOfWine", "sunfireSplinters", "bucketOfSlime"):
        yield "prayer item", prayer[key]["id"], prayer[key]["name"]
    for bone in prayer["bones"]:
        yield "prayer bone", bone["id"], bone["name"]
    for ash in prayer["ashes"]:
        yield "prayer ash", ash["id"], ash["name"]


def main():
    data = json.loads(METHODS.read_text())
    mapping = fetch_mapping()

    errors = 0
    seen = set()
    for context, item_id, name in iter_items(data):
        key = (item_id, name)
        if key in seen:
            continue
        seen.add(key)
        canonical = mapping.get(item_id) or KNOWN_NON_GE.get(item_id)
        if canonical is None:
            print(f"UNKNOWN ID  {item_id:>6}  {name!r}  (in: {context})")
            errors += 1
        elif canonical.lower() != name.lower():
            print(f"NAME DIFFERS {item_id:>6}  json={name!r} wiki={canonical!r}  (in: {context})")
            errors += 1

    print(f"\nChecked {len(seen)} unique (id, name) pairs; {errors} problem(s).")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
