"""Summarize a bounded sample of report artifacts, partitioning outcomes by tested revision."""
from collections import defaultdict
import json
import re
from pathlib import Path
import subprocess
import sys
import zipfile

from compare_execution import index
from execution_summary import EvidenceError, bounded, require


def collect_previous(destination, repo, branch, run_id):
    # Only workflow runs for this repository and PR branch. Downloads stay
    # inside disposable CI storage. Missing older artifacts remain missing.
    import urllib.parse
    repo_path = "repos/" + repo + "/actions"
    query = urllib.parse.urlencode({"branch": branch, "event": "pull_request", "per_page": 15})
    def api(endpoint):
        return json.loads(subprocess.check_output(["gh", "api", endpoint], timeout=25))
    runs = api(repo_path + "/workflows/smoke.yml/runs?" + query)["workflow_runs"]
    found = 0
    for run in runs:
        if (run["id"] >= int(run_id) or run["status"] != "completed"
                or (run.get("head_repository") or {}).get("full_name") != repo):
            continue
        artifacts = api(repo_path + "/runs/" + str(run["id"]) + "/artifacts")["artifacts"]
        pair = {}
        for a in artifacts:
            if a["expired"] or a["size_in_bytes"] > 1024*1024:
                continue
            if a["name"] in ("execution-summary-Windows", "execution-summary-Linux"):
                pair[a["name"]] = a
        if len(pair) != 2:
            continue
        for name, artifact in pair.items():
            dest = destination / str(run["id"]) / name
            dest.mkdir(parents=True, exist_ok=True)
            archive = dest / "summary.zip"
            with archive.open("wb") as stream:
                subprocess.run(["gh", "api", repo_path + "/artifacts/" + str(artifact["id"]) + "/zip"],
                               stdout=stream, check=True, timeout=30)
            with zipfile.ZipFile(archive) as bundle:
                entries = bundle.infolist()
                require(len(entries) == 1 and entries[0].filename == "execution-summary.json"
                        and entries[0].file_size <= 1024*1024, "invalid_history_archive")
                data = bundle.read(entries[0])
            report = json.loads(data)
            require(str(run["id"]) == report["workflowRun"] and report["repository"] == repo
                    and name == "execution-summary-" + report["platform"], "invalid_history_identity")
            (dest / "execution-summary.json").write_bytes(data)
        found += 1
        if found == 3:
            break
    return found


def history(reports):
    seen = set()
    revisions = defaultdict(lambda: defaultdict(list))
    executions = []
    test_history = defaultdict(list)
    for report in sorted(reports, key=lambda r: (int(r["workflowRun"]), int(r["attempt"]), r["platform"])):
        if report["schemaVersion"] != 1 or "configuration" not in report:
            continue  # Predates the comparable execution configuration contract.
        require(report["repository"] == "akshay0927/enterprise-ai-automation-framework"
                and re.fullmatch("[a-f0-9]{40}", report["testedCommit"])
                and report["platform"] in ("Linux", "Windows")
                and all(re.fullmatch("[0-9]+", report[k]) for k in ("workflowRun", "attempt"))
                and report["job"] == "smoke", "invalid_history_identity")
        compatible_inventory = True
        try:
            rows = index(report)
        except EvidenceError as failure:
            if str(failure) != "incomplete_inventory":
                raise
            legacy = dict(report, complete=False)
            rows = index(legacy)
            compatible_inventory = False
        scope = (report["repository"], report["testedCommit"], report["platform"],
                 json.dumps(report["configuration"], sort_keys=True))
        run_key = scope + (report["workflowRun"], report["attempt"], report["job"])
        require(run_key not in seen, "duplicate_execution")
        seen.add(run_key)
        executions.append({k: report[k] for k in ("testedCommit", "platform", "workflowRun", "attempt", "complete", "metrics")})
        executions[-1]["browserRetryMessages"] = report.get("observedRetryMessages") if "scriptedRetryMessages" in report else None
        executions[-1]["inventoryCompatible"] = compatible_inventory
        for key, row in rows.items():
            test_history[key].append({"run": report["workflowRun"], "attempt": report["attempt"],
                                      "platform": report["platform"], "result": row["result"],
                                      "seconds": row["seconds"], "category": row["category"]})
        if report["complete"] and compatible_inventory:
            for key, row in rows.items():
                revisions[scope][key].append(row["result"])
    signals = []
    for (_, sha, platform, _), tests in revisions.items():
        for key, values in sorted(tests.items()):
            if "passed" in values and any(v in ("failed", "error") for v in values):
                signals.append({"testedCommit": sha, "platform": platform, "test": key,
                                "sampleSize": len(values), "label": "mixed_outcomes_same_revision",
                                "passToFailure": sum(a == "passed" and b in ("failed", "error")
                                                     for a, b in zip(values, values[1:])),
                                "failureToPass": sum(a in ("failed", "error") and b == "passed"
                                                     for a, b in zip(values, values[1:]))})
    return {"schemaVersion": 1, "sampleSize": len(executions), "executions": executions,
            "mixedOutcomes": signals, "testHistory": dict(test_history)}


def main():
    import os
    root = Path("target")
    current = list((root / "platforms").glob("*/execution-summary.json"))
    require(len(current) == 2, "missing_current_pair")
    dest = root / "historical-platforms"
    available = True
    try:
        collect_previous(dest, os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_HEAD_REF"],
                         os.environ["GITHUB_RUN_ID"])
    except (subprocess.SubprocessError, OSError):
        available = False
    prior = list(dest.glob("*/*/execution-summary.json")) if available else []
    result = history([json.loads(bounded(p, 1024*1024)) for p in sorted(prior) + sorted(current)])
    result["historicalLookupAvailable"] = available
    (root / "comparison/history.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("Platform executions in history:", result["sampleSize"], "history lookup available:", available)


if __name__ == "__main__":
    main()
