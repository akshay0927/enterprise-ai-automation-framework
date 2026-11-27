"""Compare two platform reports from the same workflow attempt."""
import collections
import json
import math
from pathlib import Path
import re
import sys

from execution_summary import bounded, CLASSES, SCENARIOS, NAV, METHOD, OUTCOMES, require, FIELDS


def inventory():
    names = {"shop." + cls + "." + method for cls, methods in CLASSES.items() for method in methods}
    names.remove(NAV + "." + METHOD)
    return names | {NAV + "." + METHOD + "#" + case for case in SCENARIOS.values()}


def index(report):
    require(report["schemaVersion"] == 1 and type(report["complete"]) is bool, "unsupported_schema")
    tests = report["tests"]
    require(isinstance(tests, list) and len(tests) <= 1000, "invalid_tests")
    known = inventory()
    result = {}
    for row in tests:
        key = row["id"]
        require(key in known and key not in result, "invalid_identity")
        require(row["result"] in OUTCOMES and type(row["seconds"]) in (int, float)
                and math.isfinite(row["seconds"]) and row["seconds"] >= 0, "invalid_row")
        result[key] = row
    metrics = report["metrics"]
    if metrics is not None:
        require(set(metrics) == set(OUTCOMES) | {"total", "suiteSeconds"}, "invalid_metric_fields")
        require(all(type(metrics[k]) is int and metrics[k] >= 0 for k in (*OUTCOMES, "total"))
                and type(metrics["suiteSeconds"]) in (int, float) and math.isfinite(metrics["suiteSeconds"])
                and metrics["suiteSeconds"] >= 0, "invalid_metrics")
    retry = report["observedRetryMessages"]
    require(retry is None or (type(retry) is int and retry >= 0), "invalid_retry_count")
    if metrics is None:
        require(not tests and not report["complete"], "missing_metrics")
    else:
        counts = collections.Counter(row["result"] for row in tests)
        require(metrics["total"] == len(tests) and all(metrics[k] == counts[k] for k in OUTCOMES),
                "inconsistent_metrics")
    if report["complete"]:
        require(set(result) == known and not report["issues"], "incomplete_inventory")
    return result


def compare(windows, linux):
    require((windows["platform"], linux["platform"]) == ("Windows", "Linux"), "wrong_platforms")
    for key in ("repository", "testedCommit", "workflowRun", "attempt", "job", "schemaVersion", "configuration"):
        require(windows[key] == linux[key], "incompatible_execution")
    require(windows["repository"] == "akshay0927/enterprise-ai-automation-framework"
            and re.fullmatch("[0-9a-f]{40}", windows["testedCommit"]), "invalid_provenance")
    require(windows["configuration"] == {"browser": "chrome", "headless": True, "suite": "framework-smoke"},
            "unsupported_configuration")
    left, right = index(windows), index(linux)
    groups = collections.defaultdict(list)
    changes = []
    for key in sorted(left.keys() | right.keys()):
        a, b = left.get(key), right.get(key)
        if not a or not b:
            group = "missingWindows" if not a else "missingUbuntu"
        elif "skipped" in (a["result"], b["result"]):
            group = "skipped"
        elif a["result"] == b["result"] == "passed":
            group = "bothPassed"
        elif a["result"] != "passed" and b["result"] != "passed":
            group = "bothFailed"
        else:
            group = "windowsOnlyFailure" if b["result"] == "passed" else "ubuntuOnlyFailure"
        groups[group].append(key)
        if a and b:
            small, large = sorted((a["seconds"], b["seconds"]))
            if large - small >= 1 and large >= 2 * small:
                changes.append({"id": key, "windowsSeconds": a["seconds"], "ubuntuSeconds": b["seconds"]})
    def markers(report):
        counts = collections.Counter()
        for item in report["diagnostics"]:
            require(item.keys() == FIELDS.keys() and all(v in FIELDS[k] for k, v in item.items()), "invalid_diagnostic")
            counts[json.dumps(item, sort_keys=True)] += 1
        return counts
    win, lin = markers(windows), markers(linux)
    return {"schemaVersion": 1, "testedCommit": windows["testedCommit"],
            "complete": windows["complete"] and linux["complete"], "groups": dict(groups),
            "durationDifferences": changes, "diagnosticDifferences": {"Windows": dict(win-lin), "Linux": dict(lin-win)}}


def main():
    root = Path(sys.argv[1])
    def read(platform):
        path = root / ("execution-summary-" + platform) / "execution-summary.json"
        return json.loads(bounded(path, 1024 * 1024))
    windows, linux = read("Windows"), read("Linux")
    for report in (windows, linux):
        require(report["testedCommit"] == sys.argv[2], "checkout_mismatch")
    result = compare(windows, linux)
    out = Path("target/comparison")
    out.mkdir(parents=True, exist_ok=True)
    (out / "comparison.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    lines = ["Tested commit " + result["testedCommit"], "Complete: " + str(result["complete"])]
    for report in (windows, linux):
        lines.append(f'{report["platform"]}: {report["metrics"]}, browser retry messages {report["observedRetryMessages"]}, scripted retry messages {report["scriptedRetryMessages"]}')
    lines.extend(f"{group}: {len(items)}" for group, items in result["groups"].items())
    (out / "summary.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
