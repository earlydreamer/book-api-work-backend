-- 오늘 우리 (Today Us) 샘플 데이터 시딩 스크립트
-- 주의: Supabase SQL Editor에서 실행하세요.

-- 1. 샘플 사용자 생성 (이미 존재할 수 있으므로 auth.users 생략, public.users 연동 가정)
-- 실제로는 Supabase Auth를 통해 가입된 사용자의 ID를 사용해야 합니다.
-- 아래는 예시 코드입니다.

-- 2. 커플(Relationship) 정보 시딩 (예시 ID 사용)
-- INSERT INTO public.relationships (id, invite_code, start_date, created_at)
-- VALUES ('rel_sample_01', 'LOVE2026', '2025-01-01', now());

-- 3. 커플 멤버 연결
-- INSERT INTO public.profiles (id, relationship_id, display_name, role)
-- VALUES 
-- ('user_id_01', 'rel_sample_01', '지우', 'ME'),
-- ('user_id_02', 'rel_sample_01', '민준', 'PARTNER');

-- 4. 샘플 데일리 카드 기록 (최근 30일 중 25개 정도 채우기)
-- 이 부분은 백엔드 API를 통해 직접 쌓는 것을 권장하지만, 
-- 초기 화면 확인을 위해 SQL로 직접 넣을 경우의 예시입니다.

/* 
DO $$
DECLARE 
    i INT;
    rel_id TEXT := '실제_관계_ID'; 
BEGIN
    FOR i IN 1..30 LOOP
        INSERT INTO public.day_cards (relationship_id, local_date, me_text, partner_text, status)
        VALUES (
            rel_id, 
            CURRENT_DATE - i, 
            '오늘 ' || i || '일 전의 내 기록이야.', 
            '그날 파트너가 남긴 답장이지.',
            'COMPLETE'
        );
    END LOOP;
END $$;
*/

-- 💡 팁: 실제 프로덕션 환경에서는 프론트엔드 UI를 통해 직접 기록을 쌓거나,
-- 백엔드 테스트 코드(Integration Test)를 통해 데이터를 주입하는 것이 가장 안전합니다.
