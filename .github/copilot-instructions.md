# Framework development

Work on `feature/core-framework`. Read `IMPLEMENTATION_STATUS.md` when present and inspect the existing source before editing. Recover the synthetic shop from `framework-continuation.zip` if its source is missing.

Use a Java 21 JDK and the project Maven Wrapper. Run `sh mvnw -B clean verify` on Unix or `.\mvnw.cmd -B clean verify` on Windows. Inspect Surefire XML and confirm `guestCartIsIsolated` and `shopperAddsNotebook` each executed and passed. Record the runtime, commands, test counts, and outstanding failures in `IMPLEMENTATION_STATUS.md`.

Keep each test independent. Use loopback fixtures, synthetic users, explicit browser waits, and guaranteed cleanup. Keep credentials, cookies, and tokens out of logs and reports. Use only free authorized resources. Obtain owner approval before merging to main or deploying externally.
