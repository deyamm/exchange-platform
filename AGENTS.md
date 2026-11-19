## GitNexus Code Intelligence

This repository uses GitNexus through an HTTP MCP server running in Docker.

### Mandatory workflow

1. Before exploring unfamiliar code, check the GitNexus index and bind to the correct repository.
2. Use GitNexus `query` to locate relevant functions, classes, modules, and execution flows.
3. Use GitNexus `context` to inspect callers and dependencies before editing important symbols.
4. Use GitNexus `impact` before refactoring public interfaces or shared services.
5. Use GitNexus `trace` when investigating cross-module execution paths.
6. After editing, use `detect_changes` to inspect affected code and validate with actual tests.
7. Check index freshness. Do not trust stale graph results without verification.

### Docker execution rules

* GitNexus runs inside Docker, not directly on the Ubuntu host.
* Prefer the configured GitNexus HTTP MCP server for code intelligence.
* Do not run `npx gitnexus`, `gitnexus` or `.gitnexus/run.cjs` directly on the host.
* When a GitNexus skill instructs running `node .gitnexus/run.cjs ...`, use the host wrapper `gitnexus-docker ...` instead.
* Do not run multiple index writers against the same repository.
* Do not automatically rebuild an index during another active analysis.
* If Docker execution or MCP access fails, report the failure instead of silently assuming the index is valid.

### Development practices

* Preserve existing project architecture unless changes are required.
* Keep changes focused and review cross-module effects.
* Never treat knowledge graph results as proof of runtime correctness.
* Run appropriate tests before considering a change complete.
* Never expose credentials, secrets or sensitive configuration files.
