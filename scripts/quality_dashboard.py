"""Render a standalone dashboard from downloaded, verified execution artifacts."""
import html
import json
from pathlib import Path

from execution_summary import bounded


def read(path):
    return json.loads(bounded(path, 1024*1024))


def table(headings, rows):
    def cell(tag, value):
        return f"<{tag}>{html.escape(str(value))}</{tag}>"
    header = "".join(cell("th", h) for h in headings)
    body = "".join("<tr>" + "".join(cell("td", v) for v in row) + "</tr>" for row in rows)
    return "<table><thead><tr>" + header + "</tr></thead><tbody>" + body + "</tbody></table>"


def render(root):
    out = root / "comparison"
    comparison = read(out / "comparison.json")
    history = read(out / "history.json")
    evaluation = read(out / "evaluation.json")
    parts = ['<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">',
             '<title>Quality intelligence</title><style>body{font:16px/1.55 system-ui,sans-serif;color:#18282f;background:#fff;max-width:76rem;margin:auto;padding:2rem}h1,h2{line-height:1.2}h2{margin-top:2rem}table{display:block;overflow-x:auto;border-collapse:collapse}th,td{text-align:left;padding:.45rem .75rem;border-bottom:1px solid #ccd5d8}th{background:#eaf1f2}code{overflow-wrap:anywhere}</style><main>',
             '<h1>Quality intelligence</h1><p>Tested commit <code>' + html.escape(comparison["testedCommit"]) + '</code></p>']
    platforms, failures, advice, slow, gates = [], [], [], [], []
    for platform in ("Windows", "Linux"):
        report = read(root / "platforms" / ("execution-summary-" + platform) / "execution-summary.json")
        metrics = report["metrics"] or {}
        rate = f'{100 * metrics["passed"] / metrics["total"]:.1f}%' if report["complete"] and metrics.get("total") else "Unavailable"
        platforms.append([platform, "Complete" if report["complete"] else "Incomplete",
                          *[metrics.get(k, "—") for k in ("total", "passed", "failed", "error", "skipped", "suiteSeconds")],
                          rate, report["observedRetryMessages"] if report["observedRetryMessages"] is not None else "—"])
        for name in ("guestCartIsIsolated", "shopperAddsNotebook"):
            gates.append([platform, name, report["mandatorySmoke"].get(name, "Missing")])
        for row in report["tests"]:
            if row["result"] != "passed":
                failures.append([platform, row["id"], row["result"]])
        slow.extend([platform, r["id"], r["seconds"]] for r in report["tests"])
        analysis = read(out / ("analysis-" + platform + ".json"))
        items = list(analysis["findings"].items())
        if analysis["executionFinding"]:
            items.append(("Execution", analysis["executionFinding"]))
        for key, finding in items:
            advice.append([platform, key, finding["category"], finding["investigate"], finding["uncertainty"]])
    parts.extend(["<h2>Latest execution</h2>", table(["Platform","Evidence","Total","Passed","Failed","Errors","Skipped","Seconds","Pass rate","Browser retry messages"], platforms),
                  "<h2>Mandatory smoke tests</h2>", table(["Platform","Test","Result"], gates),
                  "<h2>Cross-platform outcomes</h2>",table(["Outcome","Tests"],[(k,len(v)) for k,v in comparison["groups"].items()]),
                  "<h2>Failed and skipped tests</h2>",table(["Platform","Test","Outcome"], failures) if failures else "<p>No failed or skipped tests were reported.</p>",
                  "<h2>Longest test invocations</h2>",table(["Platform","Test","Seconds"], sorted(slow,key=lambda r:-r[2])[:8]),
                  "<h2>Execution history</h2>",f'<p>{history["sampleSize"]} platform executions sampled. {len(history["mixedOutcomes"])} tests showed mixed outcomes on the same platform and revision.</p>',
                  table(["Run","Attempt","Platform","Commit","Evidence","Passed","Failed","Errors"],[
                      [e["workflowRun"],e["attempt"],e["platform"],e["testedCommit"][:12],"Complete" if e["complete"] else "Incomplete",
                       *[(e["metrics"] or {}).get(k,"—") for k in ("passed","failed","error")]] for e in history["executions"]]),
                  "<h2>Investigation guidance</h2>",table(["Platform","Test","Category","Investigation","Uncertainty"],advice) if advice else "<p>No current failure findings.</p>",
                  "<h2>Offline evaluation</h2>",
                  f'<p>{evaluation["correctClassifications"]} of {evaluation["sampleSize"]} synthetic scenarios classified as expected. {evaluation["appropriateAbstentions"]} appropriate abstentions. The provider evaluation uses a simulated response.</p>',
                  "</main></html>"])
    return "\n".join(parts)


if __name__ == "__main__":
    root = Path("target")
    (root / "comparison/dashboard.html").write_text(render(root), encoding="utf-8")
