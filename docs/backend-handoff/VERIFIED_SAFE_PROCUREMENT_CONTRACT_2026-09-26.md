# Verified Safe Procurement Read Contract — 2026-09-26

## Evidence

Verified in the Lovable Supabase SQL editor and from the authenticated Android app session for `narendok@gmail.com` (System Admin).

## Approved Stage 5A read projections

- `operational_purchase_requests`
- `operational_purchase_request_items`
- `operational_rfqs`
- `operational_rfq_vendors`
- `operational_vendors`
- `operational_po_shipments`
- `operational_receiving_purchase_orders`
- `operational_receiving_purchase_order_items`

Use these projections only for Stage 5A mobile reads. Do not query their raw source tables from Android.

## Verified safe fields used by Android

`operational_receiving_purchase_orders`: `id`, `po_number`, `status`, `expected_delivery_date`.

`operational_vendors`: `id`, `name`, `vendor_code`, `is_active`, `supplier_status`, `quality_risk`.

The projection also exposes operational contact and address data for receiving POs. Android must request only fields needed for the screen and must continue to exclude commercial, payment, and bank fields.

## Test evidence

The backend had zero procurement records despite the prior seed completion message. An idempotent, non-transactional vendor named `Android Sync Test Vendor` was added through Supabase. It contains no stock, approval, receiving, payment, or financial transaction. The authenticated Android Procurement screen displayed it from `operational_vendors`.

## Verified server contracts discovered

- `create_phase6_rfq(_purchase_request_id uuid, _project_id uuid, _response_due_date date, _terms text, _vendor_ids uuid[])`
- `create_purchase_order(_vendor_id uuid, _expected_delivery_date date, _notes text, _tax_amount numeric, _items jsonb)`
- `create_purchase_request_from_bom_shortage(_bom_id uuid, _required_date date)`

These are not used by the Android app yet. Any future mutation must be reviewed against the current backend handoff and remain online-only when required.
