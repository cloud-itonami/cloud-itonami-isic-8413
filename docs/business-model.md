# Business Model: Economic Regulation and Business Compliance Administration

## Vision

Economic regulatory agencies (business registration, trade licensing, export compliance, consumer protection, environmental monitoring) currently rent closed SaaS systems to manage permit applications, compliance records, and complaint escalation. These systems create vendor lock-in, obscure audit trails, and make it difficult for regulators to own their data.

This Blueprint enables an agency to fork, customize, and self-host an autonomous advisor–governor actor pair that:

1. **Intake** permit applications, registration requests, and compliance data.
2. **Verify** applicant/business records against registered databases.
3. **Log** economic statistics and compliance metrics.
4. **Escalate** complaints and violations to human staff.
5. **Audit** every decision in an append-only ledger that the agency owns.

The actor is a **document-handling and data-verification assistant**, never a decision authority. All binding permit grants, denials, tariff/rate setting, and enforcement actions remain human-only.

## Revenue Model (for operators)

Operators (regulatory agencies, development agencies, industry associations) can:

1. **Self-host** the actor on their own infrastructure.
2. **Customize** the advisor logic and business record schemas for their domain (import tax compliance, environmental monitoring, health/safety inspection).
3. **Train** their own LLM advisor or use an LLM service of choice (no vendor lock-in on the model layer).
4. **Integrate** with existing permit/licensing databases via the `Store` protocol.
5. **Resell or white-label** the actor to sub-agencies or affiliated jurisdictions (AGPL-3.0 requires source disclosure, not approval).

Revenue flows as:

- **Consulting/implementation**: helping agencies adapt the blueprint to their regulatory domain.
- **Training**: teaching regulatory staff to operate and maintain the actor.
- **Data integration**: connecting the actor to existing business registries and permit systems.
- **Hosting**: offering managed deployment for agencies without in-house infrastructure.

## Scope (What This Does)

### Intake Operations

- **Business registration verification**: accept and verify registration applications against a known registry.
- **Permit application intake**: accept permit applications, verify applicant identity, flag incompleteness or inconsistency.
- **Economic statistics collection**: log economic data (employment, revenue, trade volume) reported by businesses.
- **Complaint escalation**: accept and log compliance complaints, flag patterns, escalate to human investigators.

### Governance Model

- **Advisor proposes**: the advisor reads a request (permit application, registration verification, complaint) and proposes a verification or intake action.
- **Governor gates**: the governor audits the proposal for:
  - Business is registered (provenance check).
  - Proposal is advisory-only (`:effect :propose`).
  - Proposal is in permitted allowlist (no binding decisions).
  - Advisor confidence is high (> 0.6) or escalates.
- **Human decides**: human staff review escalated cases (all permit intakes, all complaints, low-confidence proposals).
- **Record commits**: only after human approval does the actor write to the compliance record store.
- **Ledger logs**: every proposal, verdict, and outcome is logged immutably.

## Out of Scope (What This Does NOT Do)

- **Binding permit decisions**: the actor cannot grant, deny, or revoke a permit. That is a human decision.
- **Tariff or rate setting**: the actor cannot set binding fees, tariffs, or interest rates.
- **Binding rulings**: the actor cannot issue binding legal interpretations or directives.
- **Enforcement actions**: the actor cannot apply sanctions, fines, or penalties.
- **Direct registration manipulation**: the actor cannot modify a business registration record directly; it can intake and verify only.

These are **permanent scope boundaries**, enforced architecturally (not by configuration). No approval hierarchy, no high-confidence exception, no override flag can enable them.

## Technical Stack

- **Advisor**: Clojure protocol + mock (deterministic) + LLM wrapper.
- **Governor**: Pure function (pure-functional policy layer).
- **Store**: Protocol-based (swap in Datomic, PostgreSQL, file-based ledger).
- **Actor graph**: langgraph-clj StateGraph with checkpointing for interrupt/resume.
- **Audit ledger**: Append-only log (queryable by timestamp, disposition, business-id).
- **UI**: Robotics safety console (kotoba.robotics.ui) for human review and approval.

## Deployment Scenarios

### Scenario 1: Trade Licensing Agency

An agency managing business licenses for a region:

1. Fork this blueprint and customize the advisor to recognize trade license applications.
2. Integrate the `Store` with the national business registry (read-only).
3. Deploy the actor on agency infrastructure.
4. Staff use the robotics safety console to review permit applications (all escalated) and approve/deny manually.
5. The actor logs all intake, verification, and staff decisions to an immutable ledger.

### Scenario 2: Environmental Compliance Monitoring

A ministry monitoring industrial emissions and environmental permits:

1. Customize the advisor to parse emissions reports and compliance data.
2. Integrate the `Store` with the industrial registry and permit database.
3. Deploy on a government server or cloud (no vendor lock-in).
4. Automatically flag businesses with missing or overdue compliance reports (advisor escalates).
5. Human inspectors review and approve/escalate investigations.
6. All data is owned by the ministry, not a SaaS vendor.

## Sustainability

This blueprint is **open-source (AGPL-3.0) and community-maintained**. Revenue comes from **implementation, training, and hosting services**, not from licensing or feature gates. Agencies are free to fork, customize, and self-host — and in doing so, they improve the upstream codebase with their own discoveries.

## References

- **Blueprint spec**: `blueprint.edn`
- **Architecture**: `docs/adr/0001-architecture.md`
- **Operator guide**: `docs/operator-guide.md`
- **Code**: `src/regulation/`
- **Related**: cloud-itonami-isic-8422 (defence procurement blueprint, same pattern, different domain).
