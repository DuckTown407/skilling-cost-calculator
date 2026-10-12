#!/usr/bin/env python3
"""Validate bundled recipe structure and canonical GE identities.

Run after ./gradlew test. --offline checks structure and the compile-time registry;
the default also checks the OSRS Wiki mapping. Recipe mechanics need regression fixtures.
"""
import argparse
import json
import math
import os
import re
import subprocess
import sys
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
METHODS = REPO / "src/main/resources/methods.json"
REGISTRY = REPO / "src/main/java/com/skillingcost/ResourceRegistry.java"
MAPPING_URL = "https://prices.runescape.wiki/api/v2/osrs/mapping"
SKILLS = {"SMITHING", "HERBLORE", "FISHING", "COOKING", "PRAYER", "CRAFTING",
          "FIREMAKING", "MAGIC", "FLETCHING", "RUNECRAFT", "CONSTRUCTION"}
MODES = {"GE", "FIXED_GP", "UNTRADEABLE"}


def registry_fields():
    return dict(re.findall(r'items.put\("([^"]+)", ItemID.(\w+)\);', REGISTRY.read_text()))


def iter_items(data):
    for method in data["methods"]:
        for section in ("inputs", "optional", "outputs"):
            for item in method.get(section, []):
                yield method["key"], item, True
    prayer = data["prayer"]
    for key in ("jugOfWine", "sunfireSplinters", "bucketOfSlime"):
        yield "prayer " + key, prayer[key], False
    for section in ("bones", "ashes"):
        for item in prayer[section]:
            yield "prayer " + section, item, False


def structural_errors(data, fields):
    errors = []
    if not isinstance(data, dict) or not isinstance(data.get("methods"), list) or not isinstance(data.get("prayer"), dict):
        return ["Catalog must contain methods and a Prayer table"]
    if not isinstance(data.get("skills"), list):
        return ["Catalog must contain a skill list"]
    if len(data.get("skills", [])) != 11 or set(data.get("skills", [])) != SKILLS:
        errors.append("Catalog must contain exactly the 11 supported skills")
    identities = set()
    for method in data.get("methods", []):
        key = method.get("key")
        if not key or key in identities:
            errors.append("Missing or duplicate recipe key: " + str(key))
        identities.add(key)
        if method.get("skill") not in SKILLS or not method.get("name") or not method.get("action"):
            errors.append(str(key) + ": missing skill, name, or action")
        xp = method.get("xp")
        if isinstance(xp, bool) or not isinstance(xp, (int, float)) or not math.isfinite(xp) or xp <= 0:
            errors.append(str(key) + ": XP must be positive and finite")
        level = method.get("level")
        if type(level) is not int or not 1 <= level <= 99:
            errors.append(str(key) + ": level must be 1..99")
        thresholds = method.get("outputLevels", [])
        previous = level if isinstance(level, int) else 0
        for threshold in thresholds:
            if type(threshold) is not int or not previous < threshold <= 99:
                errors.append(str(key) + ": invalid output threshold")
                break
            previous = threshold
    for context, item, quantities in iter_items(data):
        mode = item.get("pricing")
        if mode not in MODES or not item.get("name"):
            errors.append(context + ": invalid resource name or pricing mode")
        if mode == "FIXED_GP":
            fee = item.get("fixedPrice")
            if type(fee) is not int or not 0 <= fee <= 9223372036854775807 or item.get("resource"):
                errors.append(context + ": invalid fixed fee")
        elif item.get("resource") not in fields or "fixedPrice" in item:
            errors.append(context + ": unresolved resource or inconsistent pricing")
        if quantities:
            qty = item.get("qty")
            if isinstance(qty, bool) or not isinstance(qty, (int, float)) or not math.isfinite(qty) or qty < 0:
                errors.append(context + ": invalid quantity")
            capacity = item.get("unitsPerItem", 1)
            if type(capacity) is not int or capacity < 1:
                errors.append(context + ": invalid consumption capacity")
    prayer = data["prayer"]
    for section, xp_field in (("bones", "buryXp"), ("ashes", "scatterXp")):
        seen = set()
        for item in prayer[section]:
            key = item.get("resource")
            if key in seen:
                errors.append("Duplicate Prayer resource: " + str(key))
            seen.add(key)
            xp = item.get(xp_field)
            if not isinstance(xp, (int, float)) or not math.isfinite(xp) or xp <= 0:
                errors.append("Invalid Prayer XP: " + str(key))
            if not isinstance(item.get("level"), int) or not 1 <= item["level"] <= 99:
                errors.append("Invalid Prayer level: " + str(key))
            if section == "bones" and (not isinstance(item.get("shards"), int) or item["shards"] <= 0):
                errors.append("Invalid shard yield: " + str(key))
    if type(prayer.get("libationFeeGp")) is not int or not 0 <= prayer["libationFeeGp"] <= 9223372036854775807:
        errors.append("Invalid libation fee")
    if type(prayer.get("libationMinLevel")) is not int or not 1 <= prayer["libationMinLevel"] <= 99:
        errors.append("Invalid libation level")
    return errors


def compiled_constants():
    roots = [REPO / ".tooling/gradle/caches", Path.home() / ".gradle/caches"]
    jars = [jar for root in roots if root.exists() for jar in root.glob(
        "modules-2/files-2.1/net.runelite/runelite-api/*/*/runelite-api-*.jar")]
    if not jars:
        raise ValueError("Run ./gradlew test first to resolve the RuneLite API")
    java_home = os.environ.get("JAVA_HOME")
    local_jdks = list((REPO / ".tooling/jdk17").glob("jdk-*"))
    javap = Path(java_home) / "bin/javap.exe" if java_home else (
        local_jdks[0] / "bin/javap.exe" if local_jdks else Path("javap"))
    result = subprocess.run([str(javap), "-constants", "-classpath", str(max(jars, key=lambda p: p.stat().st_mtime)),
                             "net.runelite.api.gameval.ItemID"], capture_output=True, text=True,
                            check=True, timeout=30)
    return {field: int(value) for field, value in re.findall(
        r"public static final int (\w+) = (\d+);", result.stdout)}


def fetch_mapping():
    request = urllib.request.Request(MAPPING_URL, headers={
        "User-Agent": "skilling-cost-calculator method validator (contact: junk.ladybird471@proton.me)"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return {item["id"]: item["name"] for item in json.load(response)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    data = json.loads(METHODS.read_text())
    fields = registry_fields()
    errors = structural_errors(data, fields)
    count = 0
    non_ge = set()
    if not args.offline and not errors:
        constants = compiled_constants()
        mapping = fetch_mapping()
        seen = set()
        for context, item, _ in iter_items(data):
            if item["pricing"] == "FIXED_GP":
                continue
            resource = item["resource"]
            if fields[resource] not in constants:
                errors.append(context + ": gameval constant not found: " + fields[resource])
                continue
            item_id = constants[fields[resource]]
            key = (item_id, item["name"])
            if key in seen:
                continue
            seen.add(key)
            canonical = mapping.get(item_id)
            if item["pricing"] == "UNTRADEABLE":
                non_ge.add(key)
                if canonical is not None:
                    errors.append(context + ": marked untradeable but present in GE mapping: " + item["name"])
            elif canonical is None:
                errors.append(context + ": missing GE identity: " + item["name"] + " (" + str(item_id) + ")")
            elif canonical.lower() != item["name"].lower():
                errors.append(context + ": name differs: " + item["name"] + " -> " + canonical + " (" + str(item_id) + ")")
        count = len(seen)
    for error in errors:
        print(error)
    print(f"Structure checked: {len(data.get('methods', [])) if isinstance(data, dict) else 0} explicit recipes, {len(fields)} registered resources.")
    if not args.offline:
        print(f"Canonical check: {count} unique references; {len(non_ge)} explicit non-GE references.")
    print(f"{len(errors)} problem(s).")
    return 1 if errors else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.SubprocessError) as error:
        print("Validation failed: " + str(error), file=sys.stderr)
        sys.exit(1)
