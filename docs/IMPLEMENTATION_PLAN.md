# Implementation backlog

Use the current feature branch and the tested revisions in IMPLEMENTATION_STATUS.md. Keep the two mandatory smoke tests in both Java 21 wrapper jobs.

| Order | Deliverable | Acceptance evidence |
| --- | --- | --- |
| 1 | Page objects and bounded API requests | Existing shopping assertions pass through shared clients on Windows and Ubuntu |
| 2 | Product details, stock and multi-item cart | Contract and UI tests cover search, totals, quantity boundaries and removal |
| 3 | Login, logout and protected resources | Tests cover guest cart migration, expiry, revocation and user isolation |
| 4 | Checkout and order history | Synthetic checkout validates stock, rejects an empty cart and preserves order ownership |
| 5 | Regression and reporting | Reports capture actual outcomes, sanitized diagnostics and failure screenshots |
| 6 | Failure intelligence | Verified duplicate handling, retry costs and historical metrics |
| 7 | Optional AI analysis and evaluation | Offline baseline and mock provider pass labeled evaluations with measured grounding and cost |
| 8 | Read-only troubleshooting tools and dashboard | Retrieval, MCP and dashboard outputs trace back to measured results |
| 9 | Release evidence | Document reproduced commands, security findings, measured performance and portability limits |
