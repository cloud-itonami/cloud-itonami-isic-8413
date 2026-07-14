# ADR 0001: RegulationActor Architecture

## Status

ACCEPTED

## Context

ISIC Rev.5 **8413** (Economic Regulation and Business Compliance Administration) requires an autonomous actor to assist regulatory agencies with administrative workflows — business registration verification, permit application intake, economic data collection, complaint escalation — while maintaining strict separation between **advisory** (what the actor proposes) and **decision authority** (what humans approve).

The actor must enforce hard, architectural boundaries:

- The actor **never** grants, denies, or revokes a permit (human only).
- The actor **never** sets binding tariffs, rates, or fees (human only).
- The actor **never** issues binding rulings or directives (human only).
- The actor **never** applies enforcement or sanctions (human only).
- The actor **never** directly manipulates business registration records (intake and verification only).

Additionally, every proposal, verdict, and outcome must be logged to an immutable audit trail — regulators must own their own compliance records instead of renting them from a closed SaaS.

## Decision

Implement the **itonami Blueprint Actor pattern** (per ADR-2607011000 / CLAUDE.md Actors section):

### Core Components

1. **RegulationAdvisor** (protocol + mock and LLM implementations)
   - Reads a regulatory request and proposes ONE of four permitted operations.
   - Closed allowlist: `:verify-business-registration`, `:intake-permit-application`, `:log-economic-statistic`, `:flag-complaint`.
   - NEVER produces a proposal for binding decisions, enforcement, or registration manipulation.
   - Always assigns `:effect :propose` (never `:direct-write` or other actuation).
   - Assigns honest `:confidence` (0.0–1.0); LLM parse failures yield `:confidence 0.0`.

2. **RegulationGovernor** (pure function)
   - Audits every proposal against hard and escalation invariants.
   - Returns a verdict: `{:ok? bool :hard? bool :escalate? bool :violations [...]}`
   - Hard invariants (always `:hold`, no override path):
     - Business must be registered (`:no-business` violation).
     - Effect must be `:propose` (`:no-actuation` violation).
     - Operation must be in permitted allowlist (`:scope-boundary` violation).
   - Escalation invariants (always human sign-off via interrupt):
     - `:intake-permit-application` always escalates (high governance threshold).
     - `:flag-complaint` always escalates (high governance threshold).
     - Low advisor confidence (< 0.6) always escalates.

3. **RegulationStore** (protocol + MemStore)
   - Stores registered business records (`:business-id`, `:name`, `:registration-date`, `:verified-at`).
   - Stores committed compliance records (vendor-id, operation, payload).
   - Maintains an append-only audit ledger of all proposals, verdicts, and dispositions.
   - `commit-record!` is called ONLY from the actor's `:commit` node (never by the Advisor).
   - `append-ledger!` is called after every verdict (`:commit`, `:hold`, or `:request-approval`).

4. **RegulationActor** (langgraph StateGraph)
   - Entry: `:intake`
   - Advise: Advisor produces a proposal.
   - Govern: Governor audits the proposal.
   - Decide: Routes to `:commit`, `:request-approval` (interrupt-before), or `:hold` based on verdict.
   - Commit: Writes the record and appends to ledger.
   - Hold: Logs the rejection and appends to ledger (no write).
   - Checkpointing: Each superstep is checkpointed so an interrupted run can resume after human approval.

### Guaranteed Invariants

1. **No actuation without governance**: The Advisor can never directly call `store/commit-record!`. Every write is gated behind `:decide` and Governor approval.
2. **No shadow records**: All records, approvals, and rejections are logged append-only; no record can be mutated or deleted in place.
3. **Scope enforced at three layers**:
   - Advisor vocabulary (cannot construct forbidden proposals).
   - Governor logic (hard-blocks forbidden categories).
   - Audit ledger (all decisions are recorded).

## Consequences

- **Regulatory compliance**: The actor assistant is auditable and transparent; regulators own the records.
- **Safe escalation**: Permit intakes and complaints always involve human review; low-confidence advisories are never auto-committed.
- **Permanent scope boundaries**: Forbidden operations (permit grant/denial, tariff setting, binding rulings, enforcement, registration manipulation) are architecturally impossible — no configuration or approval path can enable them.
- **Auditability**: Complete chain of proposals, verdicts, and outcomes is immutable and queryable.

## Related

- ADR-2607011000: itonami Actor pattern definition.
- CLAUDE.md: Actors section.
- `blueprint.edn`: Formal specification (ISIC 8413, required capabilities, status).
- cloud-itonami-isic-8422: Reference implementation for Defence Procurement (same architecture, different domain).
