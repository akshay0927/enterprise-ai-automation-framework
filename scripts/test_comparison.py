import copy
import unittest

from compare_execution import compare
from execution_summary import OUTCOMES
import test_execution_summary as fixtures

# Build each pair with the real report generator.
class ComparisonTest(unittest.TestCase):
    def setUp(self):
        fixture = fixtures.ReportingTest("test_complete_and_distinct")
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        self.win = fixture.report()
        self.win.update(platform="Windows", repository="akshay0927/enterprise-ai-automation-framework",
                        testedCommit="a" * 40, workflowRun="123", attempt="1", job="smoke",
                        configuration={"browser": "chrome", "headless": True, "suite": "framework-smoke"})
        self.lin = copy.deepcopy(self.win)
        self.lin["platform"] = "Linux"

    def test_equal(self):
        result = compare(self.win, self.lin)
        self.assertTrue(result["complete"])
        self.assertEqual(len(result["groups"]["bothPassed"]), 16)

    def test_incompatible(self):
        for key, value in [("testedCommit", "b"*40), ("attempt", "2"), ("workflowRun", "456"),
                           ("configuration", {}), ("schemaVersion", 2), ("repository", "other")]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                compare(self.win, dict(self.lin, **{key: value}))

    def test_outcomes_and_missing(self):
        for report, values in [(self.win, ["failed", "passed", "error", "skipped"]),
                               (self.lin, ["passed", "failed", "failed", "passed"])]:
            for row, value in zip(report["tests"], values):
                row["result"] = value
        self.lin["tests"].pop()
        self.lin.update(complete=False, issues=["incomplete_test_inventory"])
        for report in (self.win, self.lin):
            for key in OUTCOMES:
                report["metrics"][key] = sum(r["result"] == key for r in report["tests"])
            report["metrics"]["total"] = len(report["tests"])
            report["mandatorySmoke"] = {r["method"]: r["result"] for r in report["tests"] if r["mandatorySmoke"]}
        self.win["tests"][0]["seconds"] = 5
        result = compare(self.win, self.lin)
        self.assertFalse(result["complete"])
        for key in ("windowsOnlyFailure", "ubuntuOnlyFailure", "bothFailed", "skipped", "missingUbuntu"):
            self.assertEqual(len(result["groups"][key]), 1)
        self.assertEqual(len(result["durationDifferences"]), 1)

    def test_unexpected_metrics_are_rejected(self):
        self.win["metrics"]["ignored"] = "SECRET_CANARY"
        with self.assertRaises(ValueError):
            compare(self.win, self.lin)

    def test_corruption(self):
        self.win["metrics"]["total"] += 1
        with self.assertRaises(ValueError):
            compare(self.win, self.lin)


if __name__ == "__main__":
    unittest.main()
