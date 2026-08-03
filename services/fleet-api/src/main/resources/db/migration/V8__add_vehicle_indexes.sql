-- =====================================================
-- Vehicle table performance indexes
-- =====================================================


-- Optimise les filtres :
-- WHERE brand = ? AND model = ?
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_model
    ON vehicle(brand, model);