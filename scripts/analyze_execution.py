"""Offline triage. A classification is a symptom supported by a report, never a changed verdict."""
import json
from pathlib import Path
import sys

from compare_execution import index
from execution_summary import bounded, FIELDS, require

GUIDANCE = {
    "blank_navigation": "Inspect the browser navigation and fixture request timing.",
    "catalog_initialization_error": "Check which catalog initialization response was last written.",
    "assertion_failure": "Compare the asserted contract with the observed fixture state.",
    "driver_mismatch": "Inspect the browser and driver versions reported by the driver factory.",
    "sign_in_failure": "Inspect the synthetic sign-in workflow and correlate its response status.",
    "toolchain_failure": "Inspect the Maven Wrapper preflight and dependency availability.",
    "unknown": "Collect a test-linked diagnostic or a typed failure signal.",
    "incomplete_execution": "Inspect the original job outcome and evidence-generation step.",
}


def finding(category, evidence, missing):
    return {"category": category, "observations": evidence, "missingEvidence": missing,
            "rootCause": None, "rootCauseConfirmed": False,
            "uncertainty": "insufficient_evidence" if category == "unknown" else "cause_unconfirmed",
            "investigate": GUIDANCE[category]}


def classify(row):
    require(row["result"] in ("failed", "error"), "expected_failed_test")
    markers = row["observations"]
    require(isinstance(markers, list) and len(markers) <= 16, "invalid_observations")
    categories = set()
    safe = []
    for item in markers:
        require(item.keys() == FIELDS.keys() and all(v in FIELDS[k] for k, v in item.items()), "invalid_marker")
        safe.append(item)
        if (item["location"] == "blank" and item["pageState"] == "unmarked"
                and all(item[k] == "false" for k in ("documentRequested", "documentSent", "productsSent", "cartSent"))):
            categories.add("blank_navigation")
        elif (item["location"] == "fixture" and item["pageState"] == "error"
              and item["documentRequested"] == item["documentSent"] == "true"):
            categories.add("catalog_initialization_error")
        else:
            categories.add("unknown")
    if not markers and row["category"] in ("driver_mismatch", "sign_in_failure"):
        return finding(row["category"], [row["category"]], ["cause"])
    if not markers and row["category"] == "assertion_failure":
        return finding("assertion_failure", ["typed_assertion"], ["cause"])
    if len(categories) == 1 and "unknown" not in categories:
        return finding(categories.pop(), safe, ["cause"])
    return finding("unknown", safe, ["consistent_test_linked_signal"])


def analyze(report):
    rows = index(report)
    findings = {key: classify(row) for key, row in rows.items() if row["result"] in ("failed", "error")}
    incomplete = finding("incomplete_execution", ["incomplete_report"], ["complete_test_evidence"])
    if report.get("toolchainCheck") == "failure" and report["metrics"] is None:
        incomplete = finding("toolchain_failure", ["toolchain_failed", "incomplete_report"], ["cause"])
    return {"schemaVersion": 1, "mode": "deterministic_offline", "findings": findings,
            "executionFinding": None if report["complete"] else
            incomplete}


def main():
    root = Path(sys.argv[1])
    output = Path("target/comparison")
    for platform in ("Windows", "Linux"):
        report = json.loads(bounded(root / ("execution-summary-" + platform) / "execution-summary.json", 1024*1024))
        result = analyze(report)
        result.update(testedCommit=report["testedCommit"], platform=platform)
        (output / ("analysis-" + platform + ".json")).write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        print(platform, "failed-test findings:", len(result["findings"]))


if __name__ == "__main__":
    main()
