import json
import tempfile
import os
import subprocess
from contextlib import chdir
import unittest
from pathlib import Path
from unittest.mock import patch
import xml.etree.ElementTree as ET

import execution_summary as es


class ReportingTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="reports with spaces ")
        self.addCleanup(temp.cleanup)
        self.directory = Path(temp.name)
        self.path = self.directory / "TEST-TestSuite.xml"
        self.suite = ET.Element("testsuite", tests="0", errors="0", failures="0", skipped="0", time="1")
        self.testng = ET.Element("testng-results")
        cls = ET.SubElement(self.testng, "class", name=es.NAV)
        for name, methods in es.CLASSES.items():
            for method in methods:
                for params in (es.SCENARIOS if method == es.METHOD else [None]):
                    ET.SubElement(self.suite, "testcase", classname="shop." + name, name=method, time="0.1")
                    if params:
                        test = ET.SubElement(cls, "test-method", name=method, status="PASS", **{"duration-ms": "100"})
                        fields = ET.SubElement(test, "params")
                        for i, value in enumerate(params):
                            ET.SubElement(ET.SubElement(fields, "param", index=str(i)), "value").text = value
        self.expected = sum(len(methods) for methods in es.CLASSES.values()) - 1 + len(es.SCENARIOS)
        self.suite.set("tests", str(self.expected))

    def report(self):
        ET.ElementTree(self.suite).write(self.path, encoding="utf-8")
        ET.ElementTree(self.testng).write(self.directory / "testng-results.xml", encoding="utf-8")
        return es.summarize(self.directory)

    def test_complete_and_distinct(self):
        report = self.report()
        self.assertTrue(report["complete"])
        self.assertEqual(report["metrics"]["passed"], self.expected)
        self.assertEqual(len({r["id"] for r in report["tests"]}), self.expected)
        self.assertEqual(report["diagnostics"], [])
        self.assertEqual(set(report["mandatorySmoke"].values()), {"passed"})

    def test_mixed_outcomes_and_redaction(self):
        for case, tag, count in zip(self.suite.findall("testcase"),
                                    ["failure", "error", "skipped"], ["failures", "errors", "skipped"]):
            ET.SubElement(case, tag, message="SECRET_CANARY").text = "SECRET_CANARY"
            ET.SubElement(case, "system-out").text = "Cookie: SECRET_CANARY"
            self.suite.set(count, "1")
        report = self.report()
        self.assertTrue(report["complete"])
        self.assertEqual([report["metrics"][k] for k in es.OUTCOMES], [self.expected - 3, 1, 1, 1])
        self.assertNotIn("SECRET_CANARY", json.dumps(report))
        self.assertEqual(report["mandatorySmoke"]["guestCartIsIsolated"], "failed")

    def test_scripted_retries_are_separate(self):
        for case in self.suite.findall("testcase"):
            if case.get("classname") == es.NAV:
                ET.SubElement(case, "system-out").text = es.RETRY
        report = self.report()
        self.assertEqual(report["observedRetryMessages"], 0)
        self.assertEqual(report["scriptedRetryMessages"], 6)

    def test_actual_checkout_provenance_even_when_reports_missing(self):
        with chdir(self.directory), patch.dict(os.environ, {
                "GITHUB_SHA": "0" * 40, "GITHUB_REPOSITORY": "example/fixture", "GITHUB_RUN_ID": "2",
                "GITHUB_RUN_ATTEMPT": "1", "GITHUB_JOB": "smoke", "RUNNER_OS": "Linux"}):
            subprocess.run(["git", "init", "--quiet"], check=True)
            subprocess.run(["git", "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                            "-c", "commit.gpgSign=false", "commit", "--allow-empty", "--quiet", "-m", "fixture"], check=True)
            want = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
            self.assertEqual(es.main(), 1)
            report = json.loads(Path("target/execution-summary.json").read_text(encoding="utf-8"))
            self.assertEqual(report["testedCommit"], want)
            self.assertIsNone(report["metrics"])
            self.assertFalse(report["complete"])

    def test_typed_failure_signals(self):
        for cls, expected in [("shop.DriverFactory$DriverVersionMismatch", "driver_mismatch"),
                              ("shop.AuthenticatedShopTest$SignInFailure", "sign_in_failure")]:
            case = self.suite.find("testcase")
            detail = ET.SubElement(case, "error", type=cls)
            self.suite.set("errors", "1")
            report = self.report()
            self.assertTrue(report["complete"])
            row = next(r for r in report["tests"] if r["result"] == "error")
            self.assertEqual(row["category"], expected)
            case.remove(detail)

    def test_untrusted_xml(self):
        for source in ("<bad", "<!DOCTYPE a><testsuite/>", "<other/>", "<testsuite/>"):
            self.path.write_text(source, encoding="utf-8")
            report = es.summarize(self.directory)
            self.assertFalse(report["complete"])
            self.assertIsNone(report["metrics"])
        self.path.unlink()
        self.assertFalse(es.summarize(self.directory)["complete"])

    def test_duplicate_or_incomplete(self):
        self.suite.set("tests", "88")
        self.assertIn("inconsistent_totals", self.report()["issues"])
        self.suite.set("tests", str(self.expected - 1))
        self.suite.remove(self.suite.find("testcase"))
        self.assertIn("incomplete_test_inventory", self.report()["issues"])
        self.suite.append(self.suite.find("testcase"))
        self.assertIn("duplicate_identity", self.report()["issues"])

    def test_diagnostics_are_independent(self):
        d = self.directory / "browser-diagnostics"
        d.mkdir()
        path = d / "catalog-open-example.txt"
        path.write_text("documentRequested=true\ndocumentSent=true\nproductsSent=true\ncartSent=false\npageState=error\nlocation=fixture\n")
        report = self.report()
        self.assertTrue(report["complete"])
        self.assertEqual(report["diagnostics"][0]["pageState"], "error")
        self.assertEqual(report["metrics"]["failed"], 0)
        path.write_text("documentRequested=SECRET_CANARY")
        report = self.report()
        self.assertFalse(report["complete"])
        self.assertEqual(report["metrics"]["passed"], self.expected)
        self.assertNotIn("SECRET_CANARY", json.dumps(report))

    def test_overlapping_files_and_limits(self):
        self.report()
        second = self.directory / "TEST-second.xml"
        second.write_bytes(self.path.read_bytes())
        self.assertFalse(es.summarize(self.directory)["complete"])
        second.unlink()
        with patch.object(es, "LIMIT", 10):
            self.assertIn("input_limit", es.summarize(self.directory)["issues"])

    def test_parameterized_error_and_ambiguous_mixture(self):
        cases = [c for c in self.suite.findall("testcase") if c.get("classname") == es.NAV]
        methods = self.testng.findall(".//test-method")
        ET.SubElement(cases[0], "error")
        self.suite.set("errors", "1")
        methods[0].set("status", "FAIL")
        report = self.report()
        self.assertTrue(report["complete"])
        self.assertEqual(report["metrics"]["error"], 1)
        ET.SubElement(cases[1], "failure")
        self.suite.set("failures", "1")
        methods[1].set("status", "FAIL")
        self.assertIn("ambiguous_invocations", self.report()["issues"])

    def test_unmapped_parameter_is_incomplete(self):
        self.testng.find(".//value").text = "SECRET_CANARY"
        report = self.report()
        self.assertIn("unknown_invocation", report["issues"])
        self.assertNotIn("SECRET_CANARY", json.dumps(report))


if __name__ == "__main__":
    unittest.main()
