-- =============================================
-- 제재 리스트 동기화 이력 테이블
-- =============================================

CREATE TABLE IF NOT EXISTS sanctions_sync_history (
    history_id BIGSERIAL PRIMARY KEY,
    
    -- 데이터 소스 (OFAC, UN)
    source_file VARCHAR(50) NOT NULL,
    
    -- 실행 상태 (SUCCESS, FAIL)
    status VARCHAR(20) NOT NULL,
    
    -- 처리 건수
    insert_count INTEGER DEFAULT 0,
    update_count INTEGER DEFAULT 0,
    unchanged_count INTEGER DEFAULT 0,
    deactivated_count INTEGER DEFAULT 0,
    total_processed INTEGER DEFAULT 0,
    
    -- 실행 시간
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    
    -- 설명 / 에러 로그 (TEXT로 전체 스택트레이스 저장)
    description TEXT,
    
    -- 다운로드 파일 크기
    file_size_bytes BIGINT
);

-- 인덱스 생성
CREATE INDEX idx_ssh_source_file ON sanctions_sync_history(source_file);
CREATE INDEX idx_ssh_status ON sanctions_sync_history(status);
CREATE INDEX idx_ssh_started_at ON sanctions_sync_history(started_at DESC);
CREATE INDEX idx_ssh_source_status ON sanctions_sync_history(source_file, status);

-- 코멘트
COMMENT ON TABLE sanctions_sync_history IS '제재 리스트 동기화 실행 이력';
COMMENT ON COLUMN sanctions_sync_history.source_file IS '데이터 소스 (OFAC, UN)';
COMMENT ON COLUMN sanctions_sync_history.status IS '실행 상태 (SUCCESS, FAIL)';
COMMENT ON COLUMN sanctions_sync_history.insert_count IS '신규 삽입 건수';
COMMENT ON COLUMN sanctions_sync_history.update_count IS '업데이트 건수';
COMMENT ON COLUMN sanctions_sync_history.unchanged_count IS '변경 없음 건수';
COMMENT ON COLUMN sanctions_sync_history.deactivated_count IS '비활성화 건수';
COMMENT ON COLUMN sanctions_sync_history.total_processed IS '총 처리 건수';
COMMENT ON COLUMN sanctions_sync_history.started_at IS '실행 시작 시간';
COMMENT ON COLUMN sanctions_sync_history.finished_at IS '실행 완료 시간';
COMMENT ON COLUMN sanctions_sync_history.duration_ms IS '소요 시간 (밀리초)';
COMMENT ON COLUMN sanctions_sync_history.description IS '설명 / 에러 로그 (실패 시 전체 스택트레이스)';
COMMENT ON COLUMN sanctions_sync_history.file_size_bytes IS '다운로드 파일 크기 (bytes)';
