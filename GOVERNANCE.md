# Governance

## itonami Blueprint Model

This repository implements the **itonami Blueprint** model for an autonomous advisor–governor actor pair. All code changes, deployments, and operational decisions are gated by the actor's built-in Governor — an independent policy layer that audits and blocks unsafe proposals, with no override mechanism for permanently forbidden operations.

See [`blueprint.edn`](blueprint.edn) for the formal specification.

## What This Governance Means

The **RegulationGovernor** is not an advisory body; it is a **hard technical barrier** enforcing a closed allowlist of permitted operations:

- **Permitted operations** (proposed by the Advisor, routed to commit after human approval):
  - Business registration verification and record intake
  - Permit application intake and documentation
  - Economic statistics data collection and logging
  - Business compliance complaint intake and escalation

- **Permanently forbidden** (will permanently `:hold`, no path to approval, no override):
  - Binding permit grant or denial
  - Setting binding tariffs, rates, or fees
  - Issuing binding regulatory rulings or directives
  - Economic sanctions or enforcement actions
  - Business registration manipulation

This is enforced at three layers:

1. **Advisor vocabulary** — the Advisor cannot construct a proposal for a forbidden operation (closed allowlist).
2. **Governor logic** — the Governor will `:hard` block any proposal touching a forbidden category, routing to `:hold` with no escalation path.
3. **Audit ledger** — every proposal, verdict, and disposition is logged append-only; audit trails are immutable.

## Contributing

All changes to the Governor logic (or any changes that might affect permitted/forbidden operation boundaries) require review by the project maintainers and must pass the test suite.

For permitted operations, contributions are welcome as long as they:

1. Do not alter the set of permitted operations without an ADR change.
2. Do not add any proposal category for forbidden operations.
3. Pass all tests and maintain audit-ledger integrity.
