-- ============================================
-- OpenWLF 제재 대상 스키마 (비정규화 버전)
-- PostgreSQL 최적화 DDL
-- ============================================

-- pg_trgm 확장 활성화 (Trigram 검색용)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================
-- 1. 제재 대상 마스터 테이블
-- ============================================
CREATE TABLE sanctions_entities (
    entity_id SERIAL PRIMARY KEY,
    
    -- 원본 식별자
    source_uid VARCHAR(100),
    source_file VARCHAR(50),           -- 'UN', 'OFAC', 'EU' 등
    entity_type VARCHAR(50),           -- 'Individual', 'Entity', 'Vessel'
    
    -- 주요 검색 필드 (정규화 완화 - 컬럼으로 전진 배치)
    primary_name TEXT,
    normalized_name TEXT,              -- 검색 최적화용 정규화된 이름
    gender VARCHAR(20),
    birth_date DATE,
    nationality VARCHAR(100),
    vessel_flag VARCHAR(100),          -- 선박인 경우
    
    -- 가변 속성 (JSONB)
    additional_features JSONB DEFAULT '{}',
    
    -- 메타데이터
    sanction_list_type VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 기본 인덱스
CREATE INDEX idx_se_source_uid ON sanctions_entities(source_uid);
CREATE INDEX idx_se_source_file ON sanctions_entities(source_file);
CREATE INDEX idx_se_entity_type ON sanctions_entities(entity_type);
CREATE INDEX idx_se_nationality ON sanctions_entities(nationality);
CREATE INDEX idx_se_birth_date ON sanctions_entities(birth_date);
CREATE INDEX idx_se_is_active ON sanctions_entities(is_active);
CREATE INDEX idx_se_sanction_list_type ON sanctions_entities(sanction_list_type);

-- 복합 인덱스 (자주 사용되는 조합)
CREATE INDEX idx_se_source_active ON sanctions_entities(source_file, is_active);
CREATE INDEX idx_se_type_nationality ON sanctions_entities(entity_type, nationality) WHERE is_active = TRUE;

-- JSONB 인덱스 (additional_features 내부 검색용)
CREATE INDEX idx_se_additional_features ON sanctions_entities USING gin (additional_features);

-- 정규화된 이름 Trigram 인덱스 (부분 일치 검색)
CREATE INDEX idx_se_normalized_name_trgm ON sanctions_entities USING gin (normalized_name gin_trgm_ops);


-- ============================================
-- 2. 이름/별칭 테이블
-- ============================================
CREATE TABLE entity_names (
    name_id SERIAL PRIMARY KEY,
    entity_id INTEGER NOT NULL REFERENCES sanctions_entities(entity_id) ON DELETE CASCADE,
    
    name_type VARCHAR(50),             -- 'Primary', 'AKA', 'FKA', 'Low Quality AKA'
    full_name TEXT NOT NULL,
    normalized_name TEXT,              -- 검색 최적화용
    script VARCHAR(50),                -- 'Latin', 'Arabic', 'Cyrillic', 'Korean' 등
    quality_score INTEGER,             -- 0-100, 높을수록 신뢰도 높음
    
    -- 개별 이름 구성요소 (선택적)
    first_name VARCHAR(200),
    middle_name VARCHAR(200),
    last_name VARCHAR(200)
);

-- 기본 인덱스
CREATE INDEX idx_en_entity_id ON entity_names(entity_id);
CREATE INDEX idx_en_name_type ON entity_names(name_type);
CREATE INDEX idx_en_script ON entity_names(script);

-- Trigram 인덱스 (핵심 - 부분 일치 검색 최적화)
CREATE INDEX idx_en_full_name_trgm ON entity_names USING gin (full_name gin_trgm_ops);
CREATE INDEX idx_en_normalized_name_trgm ON entity_names USING gin (normalized_name gin_trgm_ops);

-- 고품질 이름 필터링 인덱스
CREATE INDEX idx_en_high_quality ON entity_names(entity_id, name_type) 
    WHERE name_type <> 'Low Quality AKA';


-- ============================================
-- 3. 주소 테이블
-- ============================================
CREATE TABLE entity_addresses (
    address_id SERIAL PRIMARY KEY,
    entity_id INTEGER NOT NULL REFERENCES sanctions_entities(entity_id) ON DELETE CASCADE,
    
    address_type VARCHAR(50),          -- 'Registered', 'Residential', 'Business'
    full_address TEXT,
    street TEXT,
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    country_code VARCHAR(10),
    note TEXT
);

-- 인덱스
CREATE INDEX idx_ea_entity_id ON entity_addresses(entity_id);
CREATE INDEX idx_ea_country ON entity_addresses(country);
CREATE INDEX idx_ea_country_code ON entity_addresses(country_code);
CREATE INDEX idx_ea_city ON entity_addresses(city);

-- 주소 전문 검색 인덱스
CREATE INDEX idx_ea_full_address_trgm ON entity_addresses USING gin (full_address gin_trgm_ops);


-- ============================================
-- 4. 문서/신분증 테이블
-- ============================================
CREATE TABLE entity_documents (
    document_id SERIAL PRIMARY KEY,
    entity_id INTEGER NOT NULL REFERENCES sanctions_entities(entity_id) ON DELETE CASCADE,
    
    document_type VARCHAR(50),         -- 'Passport', 'National ID', 'Tax ID' 등
    document_number VARCHAR(100),
    issuing_country VARCHAR(100),
    issuing_country_code VARCHAR(10),
    issue_date DATE,
    expiry_date DATE,
    issuing_authority VARCHAR(200),
    note TEXT
);

-- 인덱스
CREATE INDEX idx_ed_entity_id ON entity_documents(entity_id);
CREATE INDEX idx_ed_doc_type ON entity_documents(document_type);
CREATE INDEX idx_ed_doc_number ON entity_documents(document_number);
CREATE INDEX idx_ed_issuing_country ON entity_documents(issuing_country);

-- 문서 번호 검색 인덱스
CREATE INDEX idx_ed_doc_number_trgm ON entity_documents USING gin (document_number gin_trgm_ops);


-- ============================================
-- 5. 유용한 뷰 생성
-- ============================================

-- 활성 엔티티와 Primary 이름 조회 뷰
CREATE VIEW v_active_entities AS
SELECT 
    se.entity_id,
    se.source_file,
    se.entity_type,
    se.primary_name,
    se.normalized_name,
    se.nationality,
    se.birth_date,
    se.gender,
    se.sanction_list_type,
    se.additional_features,
    se.last_updated_at
FROM sanctions_entities se
WHERE se.is_active = TRUE;

-- 엔티티별 이름 목록 뷰
CREATE VIEW v_entity_all_names AS
SELECT 
    se.entity_id,
    se.source_file,
    se.entity_type,
    en.name_type,
    en.full_name,
    en.normalized_name,
    en.script
FROM sanctions_entities se
JOIN entity_names en ON se.entity_id = en.entity_id
WHERE se.is_active = TRUE;


-- ============================================
-- 6. 통계 확인용 쿼리
-- ============================================
-- SELECT source_file, COUNT(*) FROM sanctions_entities WHERE is_active = TRUE GROUP BY source_file;
-- SELECT entity_type, COUNT(*) FROM sanctions_entities WHERE is_active = TRUE GROUP BY entity_type;
-- SELECT name_type, COUNT(*) FROM entity_names GROUP BY name_type;


-- ============================================
-- 7. 샘플 데이터 (테스트용)
-- ============================================
/*
INSERT INTO sanctions_entities (source_uid, source_file, entity_type, primary_name, normalized_name, gender, birth_date, nationality, additional_features, sanction_list_type)
VALUES 
('UN-6908330', 'UN', 'Individual', 'KIM JONG UN', 'kim jong un', 'Male', '1984-01-08', 'KP', 
 '{"titles": ["Supreme Leader"], "designations": ["UN Resolution 2270"], "placeOfBirth": "Pyongyang"}', 
 'UN Security Council Consolidated List');

INSERT INTO entity_names (entity_id, name_type, full_name, normalized_name, script)
VALUES 
(1, 'Primary', 'KIM JONG UN', 'kim jong un', 'Latin'),
(1, 'AKA', 'Kim Jong-un', 'kim jong un', 'Latin'),
(1, 'AKA', '김정은', '김정은', 'Korean');
*/
