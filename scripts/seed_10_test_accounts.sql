-- 오늘 우리 (Today Us) 실전 테스트용 10인 시딩 스크립트
-- 이 이메일 계정들의 비밀번호는 모두 'password123'입니다.
-- Supabase SQL Editor에서 실행하세요.

BEGIN;

-- 1. 기존 테스트 데이터 청소 (이전 test 계정 관련)
DELETE FROM auth.users WHERE email LIKE 'test%@test.com';
DELETE FROM public.users WHERE id IN (SELECT id::text FROM auth.users WHERE email LIKE 'test%@test.com');

-- 2. 테스트 사용자 생성 (auth.users)
-- 비밀번호 'password123'의 bcrypt 해시($2a$10$7SB76I0SgVjW.A0.0Ue9U.f6P67O6e66r6v6W66W66W66W66W66W6) 사용
DO $$
DECLARE
    u_id UUID;
    i INT;
    emails TEXT[] := ARRAY['test1@test.com', 'test2@test.com', 'test3@test.com', 'test4@test.com', 'test5@test.com', 'test6@test.com', 'test7@test.com', 'test8@test.com', 'test9@test.com', 'test10@test.com'];
    names TEXT[] := ARRAY['지우', '민준', '서연', '도윤', '하은', '예준', '지유', '시우', '수아', '주원'];
BEGIN
    FOR i IN 1..10 LOOP
        u_id := gen_random_uuid();
        
        -- Auth 유저 생성
        INSERT INTO auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at, confirmation_token, recovery_token, email_change_token_new, email_change)
        VALUES (u_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', emails[i], '$2a$10$7SB76I0SgVjW.A0.0Ue9U.f6P67O6e66r6v6W66W66W66W66W66W6', now(), '{"provider":"email","providers":["email"]}', '{}', now(), now(), '', '', '', '');

        -- Public 유저 프로필 생성
        INSERT INTO public.users (id, auth_provider, display_name, role, created_at)
        VALUES (u_id::text, 'SUPABASE', names[i], 'USER', now());
    END LOOP;
END $$;

-- 3. 커플 맺기 (5쌍)
DO $$
DECLARE
    u_ids TEXT[];
    c_id TEXT;
    i INT;
BEGIN
    u_ids := ARRAY(SELECT id FROM public.users WHERE id IN (SELECT id::text FROM auth.users WHERE email LIKE 'test%@test.com') ORDER BY id);
    
    FOR i IN 1..5 LOOP
        c_id := 'rel_pair_' || i;
        INSERT INTO public.couples (id, creator_user_id, partner_user_id, invite_code, anniversary_date, status, created_at, accepted_at)
        VALUES (c_id, u_ids[(i-1)*2+1], u_ids[(i-1)*2+2], 'COUPLE' || i, '2025-01-01', 'ACTIVE', now(), now());
    END LOOP;
END $$;

-- 4. 기록 주입 (커플별 다른 일수)
DO $$
DECLARE
    c_id TEXT;
    u1_id TEXT;
    u2_id TEXT;
    record_counts INT[] := ARRAY[25, 15, 20, 5, 30]; -- Pair 1~5의 기록 일수
    target_count INT;
    i INT;
    d INT;
    dc_id BIGINT;
    emotions TEXT[] := ARRAY['loving', 'happy', 'calm', 'excited', 'moody'];
    memos_me TEXT[] := ARRAY['오늘 날씨가 너무 좋아서 네 생각이 났어.', '카페에서 맛있는 커피 마시는 중!', '퇴근길 노을이 진짜 예쁘다.', '우리 주말에 뭐 할까?', '함께라서 행복한 하루야.'];
    memos_partner TEXT[] := ARRAY['진짜? 나도 네 생각 했는데!', '오! 나도 거기 가보고 싶어.', '사진 찍어서 보내줘!', '음... 영화 보러 갈까?', '사랑해!'];
BEGIN
    FOR i IN 1..5 LOOP
        c_id := 'rel_pair_' || i;
        target_count := record_counts[i];
        
        SELECT creator_user_id, partner_user_id INTO u1_id, u2_id FROM public.couples WHERE id = c_id;
        
        FOR d IN 0..(target_count-1) LOOP
            -- 데일리 카드 생성
            INSERT INTO public.day_cards (couple_id, local_date, state, created_at, updated_at)
            VALUES (c_id, CURRENT_DATE - (d + 1), 'COMPLETE', now() - (d || ' days')::interval, now() - (d || ' days')::interval)
            RETURNING id INTO dc_id;
            
            -- 내 기록 (Creator)
            INSERT INTO public.card_entries (day_card_id, user_id, emotion_code, memo, created_at, updated_at)
            VALUES (dc_id, u1_id, emotions[d % 5 + 1], memos_me[d % 5 + 1], now() - (d || ' days')::interval, now() - (d || ' days')::interval);
            
            -- 파트너 기록 (Partner)
            INSERT INTO public.card_entries (day_card_id, user_id, emotion_code, memo, created_at, updated_at)
            VALUES (dc_id, u2_id, emotions[(d+1) % 5 + 1], memos_partner[d % 5 + 1], now() - (d || ' days')::interval, now() - (d || ' days')::interval);
        END LOOP;
    END LOOP;
END $$;

COMMIT;

-- 💡 확인용 쿼리
-- SELECT email, display_name FROM auth.users JOIN public.users ON auth.users.id::text = public.users.id WHERE email LIKE 'test%';
-- SELECT couple_id, count(*) FROM public.day_cards GROUP BY couple_id ORDER BY couple_id;
