import copy
import unittest

from execution_history import history
from quality_dashboard import table
import test_comparison as fixtures


class HistoryTest(unittest.TestCase):
    def setUp(self):
        fixture = fixtures.ComparisonTest("test_equal")
        fixture.setUp()
        self.addCleanup(fixture.doCleanups)
        self.first = fixture.win

    def test_same_revision_and_duplicate(self):
        other = copy.deepcopy(self.first)
        other["attempt"] = "2"
        other["tests"][0]["result"] = "failed"
        other["metrics"]["failed"] += 1
        other["metrics"]["passed"] -= 1
        result = history([self.first, other])
        self.assertEqual(result["sampleSize"], 2)
        self.assertEqual(result["mixedOutcomes"][0]["sampleSize"], 2)
        other["testedCommit"] = "b" * 40
        self.assertEqual(history([self.first, other])["mixedOutcomes"], [])
        with self.assertRaises(ValueError):
            history([self.first, self.first])

    def test_missing_data_and_html(self):
        item = dict(self.first, complete=False, tests=[], metrics=None)
        self.assertIsNone(history([item])["executions"][0]["metrics"])
        self.assertNotIn("<script>", table(["Test"], [["<script>"]]))


if __name__ == "__main__":
    unittest.main()
