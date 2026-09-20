CREATE TABLE IF NOT EXISTS listing (
    id BIGSERIAL PRIMARY KEY,
    seller_sku_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    listing_data JSONB NOT NULL,
    image_urls JSONB,
    ai_response TEXT,
    excel_file VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
