"""Produce a bounded, allowlisted execution summary from this project's TestNG suite."""
import collections
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

CLASSES = {
    "SmokeTest": ["guestCartIsIsolated", "shopperAddsNotebook"],
    "AuthenticatedShopTest": ["expiredCheckoutReturnsToGuest", "browserCheckoutAndLogout"],
    "CatalogDiagnosticsTest": ["cartStartupFailureIsDiagnosed"],
    "CheckoutRaceTest": ["concurrentCheckouts"],
    "InventoryTest": ["inventoryLimitsPreserveCart"],
    "SessionOrderTest": ["isolatedOrdersAndStock"],
    "BrowserConfigTest": ["configurationPrecedence", "rejectsInvalidConfiguration"],
    "CatalogNavigationTest": ["catalogNavigationIsBounded"],
}
NAV = "shop.CatalogNavigationTest"
METHOD = "catalogNavigationIsBounded"
SCENARIOS = {
    ("READY", "[READY]", "1"): "first-ready",
    ("READY", "[BLANK, READY]", "2"): "blank-then-ready",
    ("BLANK", "[BLANK, EMPTY_DATA]", "2"): "twice-blank",
    ("RECEIVED_BUT_BLANK", "[RECEIVED_BUT_BLANK]", "1"): "already-received",
    ("ERROR", "[ERROR]", "1"): "application-error",
    ("OTHER", "[OTHER]", "1"): "other-document",
}
OUTCOMES = ("passed", "failed", "error", "skipped")
LIMIT = 8 * 1024 * 1024
RETRY = "Catalog navigation returned a blank document before reaching the fixture; retrying once."
FIELDS = {
    **dict.fromkeys(("documentRequested", "documentSent", "productsSent", "cartSent"), {"true", "false"}),
    "pageState": {"ready", "loading", "error", "unmarked", "unobserved"},
    "location": {"fixture", "blank", "other", "unavailable"},
}


class EvidenceError(ValueError):
    pass


def require(condition, code):
    if not condition:
        raise EvidenceError(code)


def bounded(path, maximum):
    with path.open("rb") as stream:
        data = stream.read(maximum + 1)
    require(len(data) <= maximum, "input_limit")
    return data.decode("utf-8-sig")


def xml(path, root):
    data = bounded(path, LIMIT)
    require("\x00" not in data and not re.search(r"<!\s*(DOCTYPE|ENTITY)", data, re.I), "unsafe_xml")
    node = ET.fromstring(data)
    require(node.tag == root, "unexpected_schema")
    return node


def seconds(value):
    require(value is not None and re.fullmatch(r"\d{1,9}(?:\.\d{1,9})?", value), "invalid_duration")
    return float(value)


def diagnostic(text):
    result = {}
    for line in text.splitlines():
        key, sep, value = line.partition("=")
        require(sep and value in FIELDS.get(key, ()) and key not in result, "invalid_diagnostic")
        result[key] = value
    require(result.keys() == FIELDS.keys(), "invalid_diagnostic")
    return result


def row(cls, method, outcome, duration, scenario=None):
    return {"id": cls + "." + method + ("#" + scenario if scenario else ""),
            "class": cls, "method": method, "result": outcome, "seconds": duration,
            "mandatorySmoke": cls == "shop.SmokeTest", "observations": [],
            "category": {"failed": "test_failure", "error": "execution_error"}.get(outcome),
            "cause": "unknown" if outcome in ("failed", "error") else None}


def navigation(directory, expected):
    root = xml(directory / "testng-results.xml", "testng-results")
    rows = []
    for cls in root.findall(".//class"):
        if cls.get("name") != NAV:
            continue
        for test in cls.findall("test-method"):
            if test.get("name") != METHOD:
                continue
            params = test.findall("params/param")
            require([p.get("index") for p in params] == ["0", "1", "2"], "invalid_parameters")
            key = tuple((p.findtext("value") or "").strip() for p in params)
            require(key in SCENARIOS, "unknown_invocation")
            status = test.get("status")
            require(status in ("PASS", "FAIL", "SKIP"), "invalid_outcome")
            outcome = {"PASS": "passed", "SKIP": "skipped", "FAIL": "failed"}[status]
            if status == "FAIL" and expected["error"] and not expected["failed"]:
                outcome = "error"
            rows.append(row(NAV, METHOD, outcome, seconds(test.get("duration-ms")) / 1000, SCENARIOS[key]))
    # TestNG combines assertion failures and execution errors. Never guess a join.
    require(collections.Counter(r["result"] for r in rows) == expected, "ambiguous_invocations")
    return rows


def summarize(directory):
    report = {"schemaVersion": 1, "complete": False, "issues": [], "tests": [], "metrics": None,
              "diagnostics": [], "mandatorySmoke": {}, "javaVersion": None, "chromeVersion": None,
              "driverVersion": None, "observedRetryMessages": None, "scriptedRetryMessages": None}
    try:
        paths = sorted(directory.glob("TEST-*.xml"))
        require(paths, "missing_suite")
        require(len(paths) == 1 and paths[0].name == "TEST-TestSuite.xml", "overlapping_or_unexpected_suite")
        suite = xml(paths[0], "testsuite")
        cases = suite.findall("testcase")
        require(0 < len(cases) <= 1000, "empty_or_oversized_suite")
        rows, totals, nav = [], collections.Counter(), collections.Counter()
        retries = scripted_retries = 0
        for case in cases:
            cls, name = case.get("classname", ""), case.get("name", "")
            methods = CLASSES.get(cls.removeprefix("shop."), [])
            require(cls.startswith("shop.") and methods, "unknown_class")
            states = [c.tag for c in case if c.tag in ("failure", "error", "skipped")]
            require(len(states) <= 1, "conflicting_outcomes")
            result = {"failure": "failed", "error": "error", "skipped": "skipped"}.get(next(iter(states), None), "passed")
            totals[result] += 1
            duration = seconds(case.get("time"))
            output = (case.findtext("system-out") or "").splitlines()
            if cls == NAV:
                scripted_retries += output.count(RETRY)
            else:
                retries += output.count(RETRY)
            for line in output:
                version = re.fullmatch(r"Chrome ([0-9.]{1,32}), ChromeDriver ([0-9.]{1,32})", line)
                if version:
                    report["chromeVersion"], report["driverVersion"] = version.groups()
            if cls == NAV:
                require(name.startswith(METHOD), "unknown_method")
                nav[result] += 1
            else:
                require(name in methods, "unknown_method")
                item = row(cls, name, result, duration)
                if states and result in ("failed", "error"):
                    detail = case.find(states[0])
                    typed = {"java.lang.AssertionError": "assertion_failure",
                             "org.opentest4j.AssertionFailedError": "assertion_failure",
                             "shop.DriverFactory$DriverVersionMismatch": "driver_mismatch",
                             "shop.AuthenticatedShopTest$SignInFailure": "sign_in_failure"}
                    item["category"] = typed.get(detail.get("type"), item["category"])
                    prefix = "Suppressed: java.lang.IllegalStateException: documentRequested="
                    for line_no, line in enumerate((detail.text or "").splitlines()):
                        if line.strip().startswith(prefix):
                            lines = (detail.text or "").splitlines()[line_no:line_no + 6]
                            lines[0] = "documentRequested=" + line.strip()[len(prefix):]
                            try:
                                observation = diagnostic("\n".join(v.strip() for v in lines))
                            except EvidenceError:
                                continue
                            item["observations"].append(observation)
                            break
                rows.append(item)
        if nav:
            rows.extend(navigation(directory, nav))
        require(len(set(r["id"] for r in rows)) == len(rows), "duplicate_identity")
        for attr, actual in (("tests", len(rows)), ("failures", totals["failed"]),
                             ("errors", totals["error"]), ("skipped", totals["skipped"])):
            require(suite.get(attr) == str(actual), "inconsistent_totals")
        report["tests"] = sorted(rows, key=lambda r: r["id"])
        report["metrics"] = {**{k: totals[k] for k in OUTCOMES}, "total": len(rows),
                             "suiteSeconds": seconds(suite.get("time"))}
        report["observedRetryMessages"] = retries
        report["scriptedRetryMessages"] = scripted_retries
        for prop in suite.findall("properties/property"):
            if prop.get("name") == "java.version" and re.fullmatch(r"[0-9.+_-]{1,32}", prop.get("value", "")):
                report["javaVersion"] = prop.get("value")
        report["mandatorySmoke"] = {r["method"]: r["result"] for r in rows if r["mandatorySmoke"]}
        expected = sum(len(v) for v in CLASSES.values()) - 1 + len(SCENARIOS)
        if len(rows) != expected:
            report["issues"].append("incomplete_test_inventory")
        diagnostics = list((directory / "browser-diagnostics").glob("catalog-open-*.txt"))
        require(len(diagnostics) <= 100, "input_limit")
        for path in diagnostics:
            report["diagnostics"].append(diagnostic(bounded(path, 512)))
        report["diagnostics"].sort(key=lambda item: json.dumps(item, sort_keys=True))
        report["complete"] = not report["issues"]
    except EvidenceError as failure:
        report["issues"].append(str(failure))
    except (ValueError, OSError, ET.ParseError):
        report["issues"].append("unreadable_evidence")
    return report


def main():
    sha = subprocess.check_output(["git", "rev-parse", "--verify", "HEAD"], text=True).strip()
    require(re.fullmatch("[0-9a-f]{40}", sha), "invalid_revision")
    summary = summarize(Path("target/surefire-reports"))
    summary.update(repository=os.environ["GITHUB_REPOSITORY"], testedCommit=sha,
                   workflowRun=os.environ["GITHUB_RUN_ID"], attempt=os.environ["GITHUB_RUN_ATTEMPT"],
                   job=os.environ["GITHUB_JOB"], platform=os.environ["RUNNER_OS"],
                   configuration={"browser": "chrome", "headless": True, "suite": "framework-smoke"},
                   generatedUtc=dt.datetime.now(dt.timezone.utc).isoformat())
    bootstrap = os.environ.get("TOOLCHAIN_OUTCOME", "unknown")
    summary["toolchainCheck"] = bootstrap if bootstrap in ("success", "failure", "skipped") else "unknown"
    path = Path("target/execution-summary.json")
    path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(summary, indent=2, allow_nan=False) + "\n", encoding="utf-8")
    print("Execution evidence:", summary["metrics"], summary["issues"])
    return 0 if summary["complete"] else 1


if __name__ == "__main__":
    sys.exit(main())
