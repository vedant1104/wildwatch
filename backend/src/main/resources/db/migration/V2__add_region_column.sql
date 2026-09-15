-- Add a dedicated region column to observations for reliable region filtering
ALTER TABLE observations
  ADD COLUMN region VARCHAR(255);

-- Note: existing rows are left NULL. New ingestion will populate this field.
