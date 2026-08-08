ALTER TABLE charges ADD CONSTRAINT uk_charge_run_unit UNIQUE (billing_run_id, unit_id);
