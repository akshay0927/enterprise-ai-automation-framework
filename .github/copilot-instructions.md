# Framework development

Work on `feature/core-framework`. Read `IMPLEMENTATION_STATUS.md` when present and inspect the existing source before editing. Reuse existing source where available. If the historical ZIP is unavailable, reconstruct missing functionality from docs/IMPLEMENTATION_PLAN.md and label its provenance in IMPLEMENTATION_STATUS.md.

Use a Java 21 JDK and the project Maven Wrapper. Run `sh mvnw -B clean verify` on Unix or `.\mvnw.cmd -B clean verify` on Windows. Inspect Surefire XML and confirm `guestCartIsIsolated` and `shopperAddsNotebook` each executed and passed. Record the runtime, commands, test counts, and outstanding failures in `IMPLEMENTATION_STATUS.md`.

Keep each test independent. Use loopback fixtures, synthetic users, explicit browser waits, and guaranteed cleanup. Keep credentials, cookies, and tokens out of logs and reports. Use only free authorized resources. Obtain owner approval before merging to main or deploying externally.
