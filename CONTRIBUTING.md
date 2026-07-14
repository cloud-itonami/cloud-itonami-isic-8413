# Contributing to cloud-itonami-isic-8413

Thank you for your interest in contributing! This project implements the itonami Blueprint model for economic regulation and business compliance administration.

## Scope

Before opening a pull request, please review the **scope boundaries** in [`README.md`](README.md) and [`GOVERNANCE.md`](GOVERNANCE.md).

### Permitted Operations

The actor can propose (subject to human approval):

- Business registration verification and record intake
- Permit application intake and documentation
- Economic statistics data collection and logging
- Business compliance complaint intake and escalation

### Permanently Forbidden

Any contribution that adds, enables, or proposes:

- Binding permit grant or denial
- Setting binding tariffs, rates, or fees
- Issuing binding regulatory rulings or directives
- Economic sanctions or enforcement actions
- Business registration manipulation

...will be rejected. These boundaries are **non-negotiable**.

## Getting Started

1. Clone this repository.
2. Install [Clojure](https://clojure.org/) and the CLI tools.
3. Run tests:
   ```bash
   clojure -M:test
   ```

## Making a Contribution

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Make your changes.
4. Run tests and ensure they pass: `clojure -M:test`.
5. Commit your changes with clear, descriptive messages.
6. Push to your fork.
7. Open a pull request to `main`.

## Testing

All changes must pass the test suite:

```bash
clojure -M:test
```

If you add new functionality, please include tests.

## Code Style

This project follows standard Clojure conventions. We use `clj-kondo` for linting (when available in your environment).

## Questions or Suggestions?

Open an issue to discuss your idea before investing significant effort.
