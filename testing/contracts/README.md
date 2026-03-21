# Event Contract Harness

This folder holds durable event-contract fixtures and lightweight reference simulators.

## Layout

- `schemas/`
  Envelope-level JSON schema references for the published Kafka wire format.
- `fixtures/`
  Golden sample envelopes for known event types and versions.
- `simulators/`
  Runnable consumer harnesses that project the published envelopes into downstream-friendly shapes for shipment, notification, and analytics testing.

## Usage

Run the simulator against any saved envelope JSON file:

```powershell
node .\testing\contracts\simulators\simulate-consumers.mjs .\testing\contracts\fixtures\order-created.v1.json
```

The script prints JSON projections for the three reference consumers.
