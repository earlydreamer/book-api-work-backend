INSERT INTO users (id, auth_provider, display_name, role, created_at, deleted_at)
VALUES
  ('local-user-1', 'local-dev', '지우', 'USER', TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00', NULL),
  ('local-user-2', 'local-dev', '민준', 'USER', TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00', NULL),
  ('local-user-3', 'local-dev', '서윤', 'USER', TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00', NULL),
  ('local-user-4', 'local-dev', '하늘', 'USER', TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00', NULL),
  ('local-user-5', 'local-dev', '도윤', 'USER', TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00', NULL);

INSERT INTO couples (
  id,
  creator_user_id,
  partner_user_id,
  invite_code,
  anniversary_date,
  status,
  created_at,
  accepted_at,
  unlinked_at
)
VALUES
  (
    'cpl_active_20260407',
    'local-user-1',
    'local-user-2',
    'TODAY2026',
    DATE '2026-04-07',
    'ACTIVE',
    TIMESTAMP WITH TIME ZONE '2026-04-07 00:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-07 00:05:00+09:00',
    NULL
  ),
  (
    'cpl_archived_20251224',
    'local-user-1',
    'local-user-3',
    'ARCHIVE2025',
    DATE '2025-12-01',
    'UNLINKED',
    TIMESTAMP WITH TIME ZONE '2025-12-01 00:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2025-12-01 00:05:00+09:00',
    TIMESTAMP WITH TIME ZONE '2025-12-31 00:00:00+09:00'
  );

INSERT INTO day_cards (
  id,
  couple_id,
  local_date,
  state,
  close_at_utc,
  closed_at,
  created_at,
  updated_at
)
VALUES
  (
    1001,
    'cpl_active_20260407',
    DATE '2026-04-07',
    'PARTIAL',
    TIMESTAMP WITH TIME ZONE '2026-04-07 19:00:00+00:00',
    NULL,
    TIMESTAMP WITH TIME ZONE '2026-04-07 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-07 09:00:00+09:00'
  ),
  (
    1002,
    'cpl_active_20260407',
    DATE '2026-04-06',
    'COMPLETE',
    TIMESTAMP WITH TIME ZONE '2026-04-06 19:00:00+00:00',
    NULL,
    TIMESTAMP WITH TIME ZONE '2026-04-06 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-06 09:00:00+09:00'
  ),
  (
    1003,
    'cpl_archived_20251224',
    DATE '2025-12-24',
    'COMPLETE',
    TIMESTAMP WITH TIME ZONE '2025-12-24 19:00:00+00:00',
    NULL,
    TIMESTAMP WITH TIME ZONE '2025-12-24 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2025-12-24 09:00:00+09:00'
  );

INSERT INTO card_entries (
  id,
  day_card_id,
  user_id,
  emotion_code,
  memo,
  photo_url,
  created_at,
  updated_at
)
VALUES
  (
    2001,
    1001,
    'local-user-1',
    'calm',
    '새 관계에서 처음 남긴 기록이에요.',
    NULL,
    TIMESTAMP WITH TIME ZONE '2026-04-07 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-07 09:00:00+09:00'
  ),
  (
    2002,
    1002,
    'local-user-1',
    'happy',
    '산책이 좋았어요.',
    'https://images.unsplash.com/photo-1511988617509-a57c8a288659?auto=format&fit=crop&w=1200&q=80',
    TIMESTAMP WITH TIME ZONE '2026-04-06 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-06 09:00:00+09:00'
  ),
  (
    2003,
    1002,
    'local-user-2',
    'loving',
    '같이 본 장면이 오래 남았어요.',
    NULL,
    TIMESTAMP WITH TIME ZONE '2026-04-06 10:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2026-04-06 10:00:00+09:00'
  ),
  (
    2004,
    1003,
    'local-user-1',
    'happy',
    '크리스마스 이브의 기록이에요.',
    NULL,
    TIMESTAMP WITH TIME ZONE '2025-12-24 09:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2025-12-24 09:00:00+09:00'
  ),
  (
    2005,
    1003,
    'local-user-3',
    'loving',
    '같이 봤던 불빛이 오래 남아요.',
    NULL,
    TIMESTAMP WITH TIME ZONE '2025-12-24 10:00:00+09:00',
    TIMESTAMP WITH TIME ZONE '2025-12-24 10:00:00+09:00'
  );

ALTER TABLE day_cards ALTER COLUMN id RESTART WITH 2000;
ALTER TABLE card_entries ALTER COLUMN id RESTART WITH 3000;
