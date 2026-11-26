# Implementation plan

## 4. Current blocker and recovery policy

The original framework-continuation.zip is unavailable.

The previous Windows workspace returned Access is denied.

Do not treat the missing ZIP as a mandatory dependency.

Immediately inspect the available repository and workspace.

If the original implementation files are available, reuse them.

If they are unavailable, reconstruct the application and automation framework from the requirements in this prompt.

Do not ask me to upload the missing archive.

Do not claim reconstructed code is recovered historical code.

If the current Windows directory is inaccessible, use an authorized user-writable project directory.

Do not elevate privileges or modify global security settings to bypass access restrictions.

## 5. Initial environment inspection

Execute appropriate commands to verify:

- Current working directory.
- Git repository root.
- Git remote.
- Current Git branch.
- Uncommitted changes.
- Latest Git commits.
- Java runtime.
- Java compiler.
- JAVA_HOME.
- Maven.
- Maven Wrapper.
- Chrome.
- GitHub authentication.
- Available memory.
- Available disk space.
- Dependency connectivity.

Use:

git status --short

git remote -v

git branch --show-current

git log -5 --oneline

java -version

javac -version

mvn -version

Preserve existing uncommitted user changes.

Verify that all subsequent changes target the correct repository.

## 6. Mandatory technology stack

Use:

- Java 21 LTS.
- Maven.
- Maven Wrapper.
- Selenium WebDriver.
- TestNG.
- REST Assured.
- Jackson.
- JSON Schema Validator.
- SLF4J.
- Compatible logging implementation.
- ExtentReports.
- GitHub Actions.

Optional technologies where justified:

- Spring Boot.
- Lightweight Java HTTP server.
- WireMock.
- Docker.
- Testcontainers.
- OpenAPI.
- PostgreSQL.
- OpenAI-compatible AI providers.
- Local AI providers.
- RAG.
- MCP.

Verify current stable dependency versions and compatibility using official sources.

Preserve the mandatory technology stack.

Avoid unnecessary architectural complexity.

## 7. Phase 1 — Reconstruct the executable foundation

Create or update:

- pom.xml
- mvnw
- mvnw.cmd
- .mvn/wrapper configuration
- .gitignore
- Shop.java
- SmokeTest.java
- TestNG configuration
- README.md
- IMPLEMENTATION_STATUS.md
- .github/workflows/ci.yml

Implement a controlled local e-commerce application.

Required functionality:

- Health endpoint.
- Product catalog.
- Notebook product.
- Product search.
- Case-insensitive search.
- Empty search results.
- Guest cart creation.
- Add product to cart.
- Cart persistence.
- Independent guest carts.
- Stable UI selectors.
- JSON REST endpoints.
- Reliable startup.
- Guaranteed shutdown.

Use synthetic products.

Do not integrate real payment systems.

Do not depend on eBay, Amazon or other third-party commercial websites.

Bind the controlled test application to loopback by default.

## 8. Phase 2 — Mandatory smoke tests

Implement and preserve these exact TestNG test method names.

### Test 1: guestCartIsIsolated

Technology: REST Assured and TestNG.

Verify:

1. Create independent guest sessions A and B.
2. Verify Guest A initially has an empty cart.
3. Add Notebook to Guest A's cart.
4. Verify Guest A's cart contains the expected product.
5. Retrieve Guest A's cart again and verify persistence.
6. Verify Guest B cannot see Guest A's cart.
7. Validate HTTP status codes.
8. Validate JSON responses.
9. Validate expected product quantity.

### Test 2: shopperAddsNotebook

Technology: Selenium and TestNG.

Verify:

1. Start the controlled application.
2. Open Chrome.
3. Navigate to the shopping application.
4. Verify catalog visibility.
5. Search for Notebook.
6. Verify matching search results.
7. Search using different letter casing.
8. Verify case-insensitive matching.
9. Search for a nonexistent product.
10. Verify empty results.
11. Search again for Notebook.
12. Add Notebook to the cart.
13. Verify the cart contains Notebook.
14. Verify the expected quantity.
15. Close the browser.
16. Stop the application.

Use dynamic explicit waits.

Never use Thread.sleep for synchronization.

Use meaningful assertions.

Ensure cleanup executes even if a test fails.

## 9. Phase 3 — Environment recovery and verification

Previous execution encountered:

- Maven Central connectivity failure.
- Chrome Target crashed.
- Native Java 21 runtime unavailable or unverified.
- Zero executed TestNG tests.

Diagnose and resolve these issues automatically.

For Maven inspect:

- DNS.
- Network connectivity.
- TLS configuration.
- Maven Wrapper distribution.
- Local dependency cache.
- Authorized proxy configuration.
- Dependency repositories.

Use official and approved artifact repositories.

Never disable certificate verification.

For Chrome inspect:

- Browser installation.
- Browser version.
- Selenium Manager.
- Browser driver compatibility.
- Headless configuration.
- Temporary profile permissions.
- Available memory.
- Browser logs.

Execute a minimal browser startup check before running the complete Selenium test.

Do not weaken browser security to suppress startup failures.

Verify native Java 21 execution.

Execute:

Windows:

.\mvnw.cmd -B clean verify

Linux/macOS:

./mvnw -B clean verify

Inspect Surefire XML reports.

Both guestCartIsIsolated and shopperAddsNotebook must actually execute and pass.

Zero executed tests is a failed quality gate.

Skipped mandatory tests are a failed quality gate.

Do not disable tests or weaken assertions.

## 10. Phase 4 — GitHub delivery

Use feature/core-framework.

Automatically:

1. Review the Git diff.
2. Verify source code contains no secrets.
3. Stage only relevant project changes.
4. Create a meaningful commit.
5. Push to the feature branch.
6. Create a draft pull request if one does not exist.
7. Trigger GitHub Actions through supported events.
8. Inspect workflow results.
9. Diagnose failures.
10. Apply fixes.
11. Commit corrective changes.
12. Verify subsequent CI execution.

If local browser or Maven execution remains blocked, use an authorized GitHub Actions environment for verification.

Do not overwrite main.

Do not force-push.

Do not merge the pull request without separate authorization.

## 11. Phase 5 — Enterprise framework architecture

After the first working vertical slice is verified, implement a maintainable modular framework.

Create appropriate packages for:

- application
- config
- driver
- waits
- pages
- components
- api
- services
- auth
- models
- data
- assertions
- exceptions
- logging
- reporting
- listeners
- utilities
- intelligence
- providers
- evaluation
- retrieval
- mcp

Prefer a modular single-module Maven project initially.

Introduce multiple Maven modules only if justified.

Apply SOLID principles, composition, encapsulation and separation of concerns.

Avoid premature abstractions.

## 12. Phase 6 — Configuration management

Implement typed configuration supporting:

- Browser type.
- Headless mode.
- Application URL.
- API URL.
- Environment.
- Explicit wait timeout.
- Page load timeout.
- API connection timeout.
- API response timeout.
- Parallel execution.
- Thread count.
- Report directory.
- Screenshot directory.
- Logging level.
- AI enablement.
- AI provider.
- AI model.

Configuration precedence:

1. System properties.
2. Environment variables.
3. Environment-specific configuration.
4. Default values.

Validate configuration before execution.

Write unit tests for configuration precedence and invalid values.

Use environment variables for secrets.

## 13. Phase 7 — Selenium infrastructure

Implement:

- DriverFactory.
- BrowserManager.
- BrowserConfig.
- BasePage.
- WaitUtils.
- ElementActions.
- ScreenshotUtils.
- Browser diagnostics.
- TestNG lifecycle management.

Support Chrome, Firefox and Edge where available.

Support headless execution.

Support Windows, macOS and Linux.

Use Selenium Manager where compatible.

Never hard-code browser driver executable paths.

Never share mutable WebDriver instances between parallel tests.

Guarantee driver.quit() after test execution.

Implement dynamic explicit waits for:

- Visibility.
- Presence.
- Clickability.
- Element disappearance.
- Text changes.
- URL changes.
- Loading completion.
- Window availability.

Avoid fixed sleeps and silent exception handling.

## 14. Phase 8 — Page Object Model

Implement:

- HomePage.
- LoginPage.
- ProductListingPage.
- ProductDetailsPage.
- CartPage.
- CheckoutPage.
- OrderConfirmationPage.
- ProfilePage.

Create reusable components:

- HeaderComponent.
- SearchComponent.
- ProductCardComponent.
- CartSummaryComponent.
- NotificationComponent.

Use stable IDs, data-testid attributes and semantic selectors.

Avoid absolute XPath and hard-coded element indexes.

Keep business assertions primarily in test classes.

Use meaningful page-level operations.

## 15. Phase 9 — Expand controlled application

Implement:

### Products

- Product categories.
- Product details.
- Product pricing.
- Stock availability.
- Out-of-stock behavior.

### Shopping cart

- Multiple products.
- Quantity updates.
- Product removal.
- Cart subtotal.
- Cart total.
- Empty cart.
- Authenticated cart.
- Guest cart migration.

### Authentication

- Login.
- Logout.
- User profile.
- Protected resources.
- Session persistence.
- Session expiration.
- User authorization.

### Orders

- Simulated checkout.
- Order creation.
- Order confirmation.
- Order details.
- Order history.

Use deterministic fixtures and synthetic users.

Document the actual application behavior and API contract.

## 16. Phase 10 — REST Assured framework

Implement:

- ApiConfig.
- RequestSpecFactory.
- ResponseSpecFactory.
- BaseApiClient.
- AuthService.
- ProductService.
- CartService.
- OrderService.
- UserService where applicable.
- Request models.
- Response models.
- Error models.
- Schema validation utilities.
- Authentication utilities.
- Test data builders.

Avoid duplicate request configuration.

Avoid unsafe global mutable REST Assured state.

Implement configurable request timeouts.

Redact passwords, cookies, tokens and authorization headers.

Use meaningful endpoint-oriented service abstractions.

## 17. Phase 11 — UI regression automation

Implement executable Selenium TestNG tests covering:

- Product catalog.
- Product search.
- Case-insensitive search.
- Empty search results.
- Special-character search.
- Product details.
- Product pricing.
- Stock availability.
- Out-of-stock products.
- Guest add-to-cart.
- Multiple product additions.
- Quantity increase.
- Quantity decrease.
- Product removal.
- Subtotal calculation.
- Total calculation.
- Empty cart.
- Guest cart persistence.
- Authenticated cart.
- Guest cart migration.
- Valid login.
- Invalid login.
- Empty credentials.
- Protected page access.
- Session persistence.
- Session expiration.
- Logout.
- Simulated checkout.
- Order confirmation.
- Order history.

Use data-driven tests where appropriate.

Create TestNG groups:

- smoke
- regression
- ui
- authentication
- security

Ensure tests can execute independently.

## 18. Phase 12 — API regression automation

Implement positive, negative and contract tests for actual endpoints.

Cover applicable:

- GET.
- POST.
- PUT.
- DELETE.
- PATCH where supported.
- Product retrieval.
- Product search.
- Product details.
- Cart retrieval.
- Product addition.
- Quantity updates.
- Product removal.
- Order creation.
- Order retrieval.
- Valid authentication.
- Invalid credentials.
- Missing credentials.
- Unauthorized resource access.
- Cross-user resource access.
- Missing access token.
- Invalid access token.
- Expired access token.
- Invalid product ID.
- Invalid quantity.
- Out-of-stock products.
- Malformed JSON.
- Invalid field types.
- Missing required fields.
- Boundary-value validation.
- HTTP status codes.
- Content-Type.
- Response headers.
- JSON schema.
- Business assertions.
- Error response structure.

Do not invent endpoints solely to increase test coverage.

## 19. Phase 13 — Authentication and security

Implement authentication using established libraries and secure framework mechanisms.

Support applicable:

- Secure credential validation.
- Session creation.
- Session expiration.
- Session invalidation.
- Bearer tokens.
- Token expiration.
- Token revocation.
- Refresh tokens where justified.
- Role-based access.
- Resource ownership validation.

Use deterministic clocks for session expiration testing where practical.

Use synthetic credentials.

Verify one user cannot access another user's cart or orders.

Do not commit real passwords or tokens.

## 20. Phase 14 — Test data and test isolation

Implement:

- TestDataFactory.
- UserDataBuilder.
- ProductDataBuilder.
- CartDataBuilder.
- OrderDataBuilder.
- Unique test identifiers.
- Deterministic fixtures.
- Per-test setup.
- Guaranteed cleanup.
- Independent browser sessions.
- Independent API sessions.

Ensure:

- Tests do not depend on execution order.
- Tests are independently executable.
- Guest carts remain isolated.
- Authenticated users remain isolated.
- Repeated executions are reliable.
- Cleanup runs after failures.

Enable parallel execution only after test and driver isolation are verified.

## 21. Phase 15 — Reporting and observability

Integrate ExtentReports and TestNG listeners.

Generate:

- HTML execution reports.
- TestNG XML reports.
- Surefire XML reports.
- Structured JSON reports.
- Failure screenshots.
- Sanitized logs.
- Failure diagnostics.

Capture:

- Test identifier.
- Test name.
- Test group.
- Execution status.
- Execution duration.
- Browser.
- Environment.
- Failure message.
- Sanitized stack trace.
- Sanitized API requests and responses.
- Retry attempts.
- Retry duration.
- Correlation ID where available.

Reporting failures must not conceal original test failures.

## 22. Phase 16 — GitHub Actions CI/CD

Implement reliable workflows for:

- Pull request validation.
- Java 21 setup.
- Maven Wrapper execution.
- Compilation.
- Unit testing.
- API smoke tests.
- Headless Selenium smoke tests.
- Regression testing.
- Authentication testing.
- Security checks.
- Report generation.
- Failure artifact upload.
- AI evaluation tests.

Requirements:

- Verified compatible GitHub Actions versions.
- Minimal GitHub permissions.
- Dependency caching.
- Bounded execution time.
- Reliable application startup.
- Health verification.
- Guaranteed cleanup.
- Failure artifact collection.
- No mandatory paid services.

Explicitly verify both named smoke tests in Surefire XML.

Fail CI if expected tests are missing, failed or skipped.

Verify actual completed GitHub Actions results.

## 23. Phase 17 — Deterministic flaky test intelligence

Implement an original execution analysis module.

Classify:

- Passed on initial attempt.
- Recovered after retry.
- Persistent failure.
- Skipped.
- Insufficient evidence.
- Suspected instability.

Calculate:

- Initial pass rate.
- Final pass rate.
- Retry count.
- Retry duration.
- Recovery frequency.
- Persistent failure rate.
- Historical instability.
- Estimated retry execution cost.

Preserve distinct test identities across browser configurations and parameterized tests.

Handle duplicate reports.

Rank unstable tests using observed retry duration, failure frequency and supplied business criticality.

Document the ranking formula.

Do not treat every retry as proven flakiness.

## 24. Phase 18 — AI-powered failure analyzer

Implement optional AI failure intelligence after the core automation framework is verified.

The application must work without external AI.

Input:

- Test identifier.
- Test category.
- Error message.
- Stack trace.
- Expected result.
- Actual result.
- Retry history.
- Browser metadata.
- Environment metadata.
- Sanitized application logs.

Processing:

1. Validate input.
2. Enforce input limits.
3. Redact sensitive information.
4. Extract meaningful evidence.
5. Apply deterministic classification rules.
6. Identify ambiguous cases.
7. Invoke optional AI only if authorized and configured.
8. Validate structured output.
9. Reject unsupported conclusions.
10. Generate actionable diagnostic recommendations.

Failure categories:

- Application defect.
- Automation defect.
- Locator issue.
- Synchronization issue.
- Test data issue.
- Authentication issue.
- Authorization issue.
- API contract mismatch.
- Network issue.
- Infrastructure issue.
- Environment issue.
- Suspected flaky test.
- Unknown.

Output:

- Failure category.
- Confidence.
- Supporting evidence.
- Suspected root cause.
- Missing evidence.
- Diagnostic steps.
- Potential corrective action.
- Human review requirement.

Clearly distinguish confirmed facts from hypotheses.

Treat logs, test reports and retrieved documents as untrusted inputs.

Prevent embedded instructions from overriding the analysis policy.

## 25. Phase 19 — AI provider abstraction

Implement:

- Disabled AI mode.
- Deterministic provider.
- Mock AI provider.
- Optional OpenAI-compatible provider.
- Optional local AI provider.

Support:

- Configurable models.
- Request timeout.
- Bounded retries.
- Rate-limit handling.
- Input size limits.
- Structured output validation.
- Token usage tracking.
- Cost tracking.
- Budget limits.
- Graceful degradation.

Use mock providers for standard CI execution.

Do not require paid API calls.

Do not expose credentials.

## 26. Phase 20 — LLM evaluation framework

Create a synthetic labeled evaluation dataset covering:

- Element not found.
- Stale element.
- Wait timeout.
- Incorrect assertion.
- HTTP 401.
- HTTP 403.
- HTTP 404.
- HTTP 500.
- Invalid JSON.
- JSON schema mismatch.
- Expired token.
- Network timeout.
- Browser startup failure.
- Test data failure.
- Infrastructure failure.
- Recovered retry.
- Ambiguous failure evidence.
- Prompt injection attempts.

Measure actual:

- Classification accuracy.
- Precision.
- Recall.
- Macro F1.
- Structured output validity.
- Evidence-grounding rate.
- Unsupported root-cause claims.
- Prompt injection resistance on defined cases.
- Analysis latency.
- Model cost where applicable.

Separate development examples from evaluation examples.

Compare deterministic, optional AI and hybrid approaches only when actually executed.

Never fabricate evaluation results.

## 27. Phase 21 — RAG integration

Implement retrieval-assisted troubleshooting where useful.

Create an original synthetic knowledge base containing:

- Troubleshooting guides.
- Automation failure patterns.
- Architecture documentation.
- Known synthetic defects.
- Framework best practices.

Implement:

- Document ingestion.
- Source metadata.
- Retrieval.
- Evidence-backed recommendations.
- Source attribution.
- Retrieval evaluation.

Start with simple local retrieval.

Introduce embeddings and vector databases only when measurable requirements justify them.

Do not ingest confidential employer or client information.

## 28. Phase 22 — MCP integration

Implement a useful read-only MCP server.

Potential tools:

- get_test_summary
- list_failed_tests
- analyze_failure
- get_flaky_test_ranking
- get_retry_cost
- get_failure_history
- search_troubleshooting_knowledge

Implement:

- Typed input schemas.
- Typed output schemas.
- Argument validation.
- Error handling.
- Tool execution tests.
- Local configuration.
- Secret protection.
- Documentation.

Demonstrate actual MCP tool execution.

Do not expose production write-capable tools or confidential artifacts.

## 29. Phase 23 — Quality intelligence dashboard

Build a lightweight dashboard using actual execution data.

Display:

- Total tests.
- Passed tests.
- Failed tests.
- Skipped tests.
- Initial pass rate.
- Final pass rate.
- Retry count.
- Retry duration.
- Recovered tests.
- Persistent failures.
- Suspected flaky tests.
- Failure categories.
- UI execution duration.
- API execution duration.
- AI evaluation accuracy.
- AI analysis latency.
- AI cost where applicable.

Provide meaningful filters.

Document metric definitions.

Start with generated HTML.

Introduce database persistence only when justified.

Never fabricate production metrics.

## 30. Phase 24 — Docker and reproducibility

Evaluate Docker based on practical need.

Where justified, implement:

- Dockerfile.
- Docker Compose.
- Health checks.
- Environment configuration.
- Reproducible application startup.
- Automated test execution.
- Guaranteed container cleanup.

Maintain non-Docker Maven execution.

Do not require paid infrastructure.

Verify actual container builds where available.

## 31. Phase 25 — Security audit

Review:

- Hard-coded credentials.
- API key exposure.
- Vulnerable dependencies.
- Unsafe logging.
- Unsafe deserialization.
- Unsafe report parsing.
- HTML injection.
- Path traversal.
- Authentication weaknesses.
- Authorization weaknesses.
- Cross-user resource access.
- Excessive GitHub permissions.
- Prompt injection.
- Unsafe AI tool execution.
- Unnecessary network requests.

Fix verified defects.

Record unresolved security findings.

Do not claim complete security.

## 32. Phase 26 — Professional documentation

Create and maintain:

- README.md.
- IMPLEMENTATION_STATUS.md.
- SECURITY.md.
- CONTRIBUTING.md.
- Architecture diagrams.
- Architecture decision records.
- API contracts.
- Authentication lifecycle documentation.
- Test strategy.
- CI/CD guide.
- AI architecture.
- Evaluation methodology.
- RAG documentation.
- MCP documentation.
- Troubleshooting guide.
- Developer onboarding guide.

Create a professional portfolio case study suitable for:

- Senior SDET.
- QA Architect.
- Production AI Engineer.
- Forward Deployed Engineer.

Document actual technical decisions, test results and reproducible commands.

Do not invent customer adoption or production usage.

## 33. Persistent Copilot instructions

Expand:

.github/copilot-instructions.md

Include permanent rules for:

- Architecture.
- Mandatory technology stack.
- Coding standards.
- Dynamic waits.
- Testing.
- Security.
- Git workflow.
- Autonomous execution.
- Cost limits.
- Quality gates.
- Definition of done.

Create:

docs/IMPLEMENTATION_PLAN.md

Maintain an ordered backlog of incomplete requirements.

Maintain:

IMPLEMENTATION_STATUS.md

Record:

- Current milestone.
- Completed tasks.
- Pending tasks.
- Files modified.
- Build commands.
- Actual compilation results.
- Tests executed.
- Passed tests.
- Failed tests.
- Skipped tests.
- Commit SHAs.
- CI status.
- Known defects.
- Outstanding blockers.
- Next executable task.

Read these files at the beginning of every new Copilot session.

Resume from the latest verified milestone.

Never unnecessarily restart the project.

## 34. Autonomous implementation algorithm

WHILE authorized executable work remains:

1. Inspect the actual repository.
2. Read the implementation tracker.
3. Select the highest-priority incomplete task.
4. Implement actual source code.
5. Compile affected components.
6. Execute relevant tests.
7. Inspect failures.
8. Diagnose causes.
9. Apply corrective fixes.
10. Re-run verification.
11. Review security implications.
12. Review Git differences.
13. Commit verified changes.
14. Update the implementation tracker.
15. Continue to the next task.

END when all quality gates pass or genuine limitations prevent further meaningful work.

Do not stop after completing one phase.

Do not ask whether to continue.

Do not generate another roadmap instead of implementation.

## 35. Automatic blocker recovery

If an operation fails:

- Capture the exact error.
- Inspect relevant logs.
- Diagnose the issue.
- Investigate legitimate alternatives.
- Apply a minimal corrective change.
- Re-run the operation.
- Record actual results.

Avoid repeating identical failed actions.

If GitHub writes are blocked, continue local implementation.

If Chrome is blocked, continue API verification.

If Maven access is blocked, perform independently executable verification and recover dependency access.

If external AI is unavailable, continue deterministic and mock-provider implementation.

If one task remains blocked, continue independent tasks.

Do not accumulate large amounts of unverified code.

## 36. Quality gates

### Gate 1 — Foundation

- Native Java 21 execution.
- Successful Maven build.
- Controlled application starts.
- Both mandatory smoke tests execute and pass.
- TestNG reports generated.

### Gate 2 — Enterprise framework

- Typed configuration.
- Selenium architecture.
- REST Assured architecture.
- Authentication.
- UI regression.
- API regression.
- Test isolation.

### Gate 3 — Delivery

- Feature branch commits.
- Pull request.
- Successful GitHub Actions execution.
- Failure artifacts.
- Security review.

### Gate 4 — AI intelligence

- Deterministic failure analyzer.
- Flaky test metrics.
- Optional AI provider abstraction.
- Mock provider.
- Structured outputs.
- Evaluation dataset.
- Actual evaluation results.

### Gate 5 — Portfolio readiness

- Useful RAG and MCP capabilities where justified.
- Quality dashboard.
- Reproducible execution.
- Professional documentation.
- Verified delivery.

Never mark a gate complete without actual evidence.

## 37. Security and financial boundaries

Maximum incremental expenditure: ₹0.

Respect existing Copilot usage limits.

Do not:

- Purchase credits or subscriptions.
- Use paid AI APIs.
- Enable paid infrastructure.
- Expose credentials.
- Access unauthorized employer resources.
- Modify unrelated repositories.
- Force-push.
- Delete important data.
- Disable security controls.
- Bypass mandatory permissions.
- Merge into main without approval.
- Deploy externally without authorization.

Use independently owned source code and synthetic data.

Automatically skip optional paid functionality and continue free alternatives.

## 38. Zero routine intervention policy

Do not ask me to make ordinary engineering decisions.

Automatically select:

- Package names.
- Class names.
- Directory structures.
- Compatible dependencies.
- Configuration defaults.
- Test scenarios.
- Test data.
- Refactoring strategies.
- Error handling.
- Debugging approaches.
- Commit messages.
- Implementation sequence.

Only require intervention for unavoidable mandatory authentication, platform permissions, financial commitments, destructive changes or operations outside the authorized scope.

Consolidate unavoidable blockers into one concise report.

Continue all independent authorized tasks.

## 39. Session continuation

If execution limits are reached:

- Save all actual source changes.
- Commit verified changes where authorized.
- Update IMPLEMENTATION_STATUS.md.
- Record the exact next executable task.
- Preserve reproducible build and verification commands.

Do not claim background work is running unless an actual supported mechanism confirms it.

Avoid concurrent changes to the same branch by another scheduled agent.

## 40. Final reporting

Report actual:

- Features implemented.
- Files created or modified.
- Commands executed.
- Build results.
- Tests executed.
- Tests passed.
- Tests failed.
- Tests skipped.
- Commit SHAs.
- Feature branch URL.
- Pull request URL.
- CI workflow status.
- AI evaluation results.
- Remaining blockers.
- Next executable task.

Do not fabricate results.

## 41. START AUTOPILOT NOW

Your immediate tasks are:

1. Inspect the existing repository.
2. Recover accessible implementation files.
3. Reconstruct missing source without waiting for the ZIP.
4. Implement the controlled e-commerce application.
5. Implement guestCartIsIsolated.
6. Implement shopperAddsNotebook.
7. Establish native Java 21 execution.
8. Execute Maven clean verify.
9. Diagnose and fix actual failures.
10. Commit the working foundation.
11. Push to feature/core-framework.
12. Create or update the pull request.
13. Verify GitHub Actions.
14. Automatically implement every remaining enterprise and AI engineering milestone.

Use actual tools.

Write actual code.

Execute actual commands.

Run actual tests.

Fix actual defects.

Commit actual improvements.

Continue autonomously without requesting routine approval.

BEGIN COMPLETE AUTOPILOT IMPLEMENTATION NOW.

## How to use it

**Step 1 — Select Agent mode**

Open your project in VS Code, open Copilot Chat and select Agent rather than Ask mode.

---

**Step 2 — Enable Autopilot**

Select Autopilot if supported by your installed version and account. Keep permissions scoped to your project.

---

**Step 3 — Execute**

Paste the master prompt and allow Copilot to implement, compile, test and fix the project.

## Expected implementation pipeline

Repository and feature branch  Completed

Persistent Copilot instructions  Completed

Reconstruct missing implementation  Immediate priority

Java 21 and Maven verification  Pending

Selenium and REST Assured  Pending

Enterprise automation architecture  Pending

Authentication and regression  Pending

Reporting and CI/CD  Pending

AI failure intelligence  Pending

LLM evaluations  Pending

RAG and MCP  Pending

Dashboard and portfolio delivery  Pending

