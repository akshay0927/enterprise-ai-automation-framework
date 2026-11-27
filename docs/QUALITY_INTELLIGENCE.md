# Quality intelligence demo

Open the **execution-comparison** artifact from the framework workflow and open `dashboard.html` in a browser. The dashboard runs offline. Its JSON siblings contain the comparison, per-platform findings, interpretations, labeled evaluation and bounded execution history. Each platform also uploads its independently generated `execution-summary.json`. The actual checkout SHA can be the GitHub pull-request merge revision.

## Walk through an observed failure

[Run 45994266355](https://github.com/akshay0927/enterprise-ai-automation-framework/actions/runs/45994266355) compared the same checkout on two platforms. Windows reported 16 passes. The Ubuntu Maven Wrapper could not fetch its distribution and produced no Surefire XML. The comparison marked 16 Ubuntu cases missing. Ubuntu has neither a measured pass rate nor a failed test assertion for that attempt. Start the investigation at the wrapper download step.

For the opposite platform symptom, [Windows job 140471397626](https://github.com/akshay0927/enterprise-ai-automation-framework/actions/runs/45988892386/job/140471397626) recorded blank navigation before any document request. That run predates structured summaries. It is historical log evidence only.

## Reproduce

Run the Java 21 Maven Wrapper. In the GitHub workflow the reporter obtains its revision from `git rev-parse HEAD` and records repository, run, attempt, job and OS. To regenerate locally, set `GITHUB_REPOSITORY`, `GITHUB_RUN_ID`, `GITHUB_RUN_ATTEMPT`, `GITHUB_JOB` and `RUNNER_OS` to the matching execution metadata before running `python scripts/execution_summary.py` on the corresponding checkout.

Place downloaded platform summary artifacts under `target/platforms/execution-summary-Windows/` and `target/platforms/execution-summary-Linux/`. Run `python scripts/compare_execution.py target/platforms <tested-commit-sha>`. Duration differences require both a one-second absolute difference and a twofold relative difference. The historical sample includes at most three earlier artifact-bearing PR runs plus the current pair. Mixed outcomes require repeated complete reports for the same test, platform, configuration and tested revision. A single failure cannot establish instability.

Failure classifications depend on structured, test-linked evidence. Missing signals produce an unknown cause. Catalog files gathered from controlled failures remain execution-level observations. The optional provider accepts a grounded candidate or abstains; CI evaluates a simulated provider. The eight-case labeled synthetic set measures this contract only. No hosted inference, latency benchmark or commercial savings claim is implied.
