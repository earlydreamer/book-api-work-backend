CREATE TABLE uploaded_assets (
  id VARCHAR(100) PRIMARY KEY,
  owner_user_id VARCHAR(100) NOT NULL,
  couple_id VARCHAR(100) NOT NULL,
  object_key VARCHAR(500) NOT NULL UNIQUE,
  public_url VARCHAR(2000) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  uploaded_at TIMESTAMP WITH TIME ZONE NULL,
  CONSTRAINT fk_uploaded_assets_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id),
  CONSTRAINT fk_uploaded_assets_couple FOREIGN KEY (couple_id) REFERENCES couples (id)
);

CREATE INDEX idx_uploaded_assets_owner_user_id ON uploaded_assets (owner_user_id);
CREATE INDEX idx_uploaded_assets_couple_id ON uploaded_assets (couple_id);
CREATE INDEX idx_uploaded_assets_status ON uploaded_assets (status);

ALTER TABLE card_entries
  ADD COLUMN uploaded_asset_id VARCHAR(100) NULL;

ALTER TABLE card_entries
  ADD CONSTRAINT fk_card_entries_uploaded_asset
  FOREIGN KEY (uploaded_asset_id) REFERENCES uploaded_assets (id);

CREATE INDEX idx_card_entries_uploaded_asset_id ON card_entries (uploaded_asset_id);
