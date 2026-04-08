ALTER TABLE book_snapshot_items 
ADD COLUMN me_memo TEXT NULL,
ADD COLUMN partner_memo TEXT NULL,
ADD COLUMN me_display_name VARCHAR(100) NULL,
ADD COLUMN partner_display_name VARCHAR(100) NULL;
