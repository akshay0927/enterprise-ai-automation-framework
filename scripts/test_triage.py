import unittest

from analyze_execution import classify, analyze
from execution_summary import FIELDS
import test_execution_summary as fixtures


class TriageTest(unittest.TestCase):
    def test_evidence_and_abstention(self):
        blank = dict.fromkeys(list(FIELDS)[:4], "false")
        blank.update(location="blank", pageState="unmarked")
        error = dict(blank, location="fixture", documentRequested="true", documentSent="true", pageState="error")
        for category, markers, expected in [
            ("test_failure", [blank], "blank_navigation"),
            ("test_failure", [error], "catalog_initialization_error"),
            ("assertion_failure", [], "assertion_failure"),
            ("test_failure", [blank, error], "unknown"),
            ("test_failure", [dict(blank, cartSent="true")], "unknown"),
            ("execution_error", [], "unknown"),
        ]:
            with self.subTest(expected=expected):
                result = classify({"result": "failed", "category": category, "observations": markers})
                self.assertEqual(result["category"], expected)
                self.assertIsNone(result["rootCause"])
                self.assertFalse(result["rootCauseConfirmed"])

    def test_green_controlled_failure_and_incomplete_report(self):
        fixture = fixtures.ReportingTest("test_complete_and_distinct")
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        report = fixture.report()
        self.assertEqual(analyze(report)["findings"], {})
        report["complete"] = False
        self.assertEqual(analyze(report)["executionFinding"]["category"], "incomplete_execution")

    def test_toolchain_failure_requires_both_signals(self):
        fixture = fixtures.ReportingTest("test_complete_and_distinct")
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        report = fixture.report()
        report.update(complete=False, metrics=None, tests=[], toolchainCheck="failure")
        self.assertEqual(analyze(report)["executionFinding"]["category"], "toolchain_failure")
        report["toolchainCheck"] = "unknown"
        self.assertEqual(analyze(report)["executionFinding"]["category"], "incomplete_execution")

    def test_rejects_raw_observations(self):
        with self.assertRaises(ValueError):
            classify({"result": "error", "category": "execution_error", "observations": [{"cookie": "SECRET"}]})


if __name__ == "__main__":
    unittest.main()
