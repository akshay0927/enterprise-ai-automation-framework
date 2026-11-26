# Project engineering

Read IMPLEMENTATION_STATUS.md, docs/IMPLEMENTATION_PLAN.md and .github/copilot-instructions.md before editing. Resume from verified source on feature/core-framework. Preserve the two named smoke tests. Use per-test loopback fixtures and synthetic data. Verify changes through the Java 21 wrapper build on both CI platforms. Record test evidence and unresolved coverage in the tracker. Keep main merges and paid services subject to owner approval.

For each change inspect the diff, identify a failure case and verify its observable outcome. Review credential handling, resource ownership, partial writes and cleanup when relevant. Correct defects, rerun affected tests and record the tested revision and residual limitations. Continue from the highest incomplete backlog item within the authorized run budget.
