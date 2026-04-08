-- sweetbook_books 테이블의 단일 template_id 구조를 다중 템플릿 구조로 확장
ALTER TABLE sweetbook_books ADD COLUMN cover_template_id VARCHAR(100);
ALTER TABLE sweetbook_books ADD COLUMN content_template_id VARCHAR(100);
ALTER TABLE sweetbook_books ADD COLUMN interleaf_template_id VARCHAR(100);
ALTER TABLE sweetbook_books ADD COLUMN publishing_template_id VARCHAR(100);

-- 기존 template_id 데이터를 content_template_id로 마이그레이션 (필요시)
UPDATE sweetbook_books SET content_template_id = template_id;

-- 기존 template_id 컬럼은 하위 호환성을 위해 유지하거나 나중에 삭제 (일단 유지)
-- NOT NULL 제약조건은 새로운 컬럼들로 이관
ALTER TABLE sweetbook_books ALTER COLUMN cover_template_id SET NOT NULL;
ALTER TABLE sweetbook_books ALTER COLUMN content_template_id SET NOT NULL;
