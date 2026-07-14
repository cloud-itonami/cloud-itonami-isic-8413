# Security Policy

## Scope Boundaries as Security Features

This actor's security model is built on **explicit scope boundaries**. The Governor enforces a closed allowlist of permitted operations and will permanently block any proposal that touches forbidden categories:

- Binding permit grant or denial
- Setting binding tariffs, rates, or fees
- Issuing binding regulatory rulings or directives
- Economic sanctions or enforcement actions
- Business registration manipulation

These are not "dangerous but escalatable" operations — they are **architecturally impossible** in the actor's design.

## Reporting a Vulnerability

If you discover a vulnerability in the Governor logic, audit ledger implementation, or scope-boundary enforcement:

1. **Do not open a public issue.**
2. Email the project maintainers with a detailed description of the issue.
3. Include reproduction steps and impact assessment.
4. Allow reasonable time for a response and fix before public disclosure.

## Audit Ledger Integrity

All operations (permitted or forbidden) are logged to an append-only audit ledger. The ledger is immutable once written. If you discover:

- A way to bypass the Governor and directly commit a record
- A way to mutate or delete audit ledger entries
- A way to fabricate a false audit trail

...these are critical security issues and should be reported immediately.

## Dependencies

This project depends on:

- `io.github.kotoba-lang/langgraph` (pinned by git/sha)
- `io.github.cognitect-labs/test-runner` (pinned by git/tag)

All dependencies are specified with exact pins in `deps.edn`. Monitor upstream repositories for security updates and report any discovered vulnerabilities.

## Security-Relevant Changes

Any change to:

- The Governor's hard-invariant logic
- The permitted-operation allowlist
- The store's commit or ledger semantics
- The actor graph structure or node routing

...is a security-relevant change and should include:

1. A clear explanation of the change's security implications.
2. Test coverage demonstrating the security property.
3. Review from the project maintainers.

Thank you for helping us keep this project secure.
