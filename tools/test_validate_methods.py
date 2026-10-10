"""Offline validator regressions; no live prices or network requests."""
import copy
import json
import unittest
from validate_methods import METHODS, registry_fields, structural_errors


class CatalogValidationTest(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(METHODS.read_text())
        self.fields = registry_fields()

    def test_current_catalog_and_explicit_non_ge_modes(self):
        self.assertEqual([], structural_errors(self.data, self.fields))
        modes = {item["pricing"] for method in self.data["methods"]
                 for section in ("inputs", "optional", "outputs") for item in method[section]}
        self.assertEqual({"GE", "FIXED_GP", "UNTRADEABLE"}, modes)

    def test_duplicate_recipe_unknown_resource_and_missing_mode(self):
        duplicate = copy.deepcopy(self.data)
        duplicate["methods"].append(duplicate["methods"][0])
        self.assertTrue(any("duplicate" in error for error in structural_errors(duplicate, self.fields)))
        self.data["methods"][0]["inputs"][0]["resource"] = "DOES_NOT_EXIST"
        self.assertTrue(any("unresolved" in error for error in structural_errors(self.data, self.fields)))
        self.data["methods"][0]["inputs"][0].pop("pricing")
        self.assertTrue(any("pricing mode" in error for error in structural_errors(self.data, self.fields)))

    def test_invalid_xp_levels_quantities_and_fixed_fees(self):
        for key, value in (("xp", 0), ("xp", float("inf")), ("xp", True), ("level", 100)):
            data = copy.deepcopy(self.data)
            data["methods"][0][key] = value
            self.assertTrue(structural_errors(data, self.fields), (key, value))
        for key, value in (("qty", -1), ("qty", True), ("unitsPerItem", 0)):
            data = copy.deepcopy(self.data)
            data["methods"][0]["inputs"][0][key] = value
            self.assertTrue(structural_errors(data, self.fields), (key, value))
        fee = {"name": "fee", "qty": 1, "pricing": "FIXED_GP", "fixedPrice": -1}
        self.data["methods"][0]["inputs"] = [fee]
        self.assertTrue(any("fixed fee" in error for error in structural_errors(self.data, self.fields)))

    def test_skill_scope_thresholds_and_invalid_envelope(self):
        self.data["skills"].append("FARMING")
        self.assertTrue(structural_errors(self.data, self.fields))
        self.data["skills"].pop()
        self.data["methods"][0]["outputLevels"] = [99, 99]
        self.assertTrue(any("threshold" in error for error in structural_errors(self.data, self.fields)))
        self.assertTrue(structural_errors({}, self.fields))
        self.assertTrue(structural_errors(None, self.fields))


if __name__ == "__main__":
    unittest.main()
