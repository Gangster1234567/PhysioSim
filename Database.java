// DB 연결 관리
package physiosim.db;

import java.sql.*;

public class Database {

    //  DB 파일 경로 (상수로 고정): 캡슐화(정보 은닉) + 상수화(값 고정)
    private static final String DB_FILE = "data/app.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    // PRAGMA: 동시성/신뢰성 균형을 위한 타임아웃(ms)
    private static final int BUSY_TIMEOUT_MS = 5000;
    
    private Connection conn;

    // 현재 연결 상태 확인
    public boolean isOpen() {
        try { return conn != null && !conn.isClosed(); }
        catch (SQLException e) { return false; }
    }

    // 외부에서 커넥션 호출 (열려있지 않으면 예외)
    public Connection getConnection() {
        if (!isOpen()) {
            throw new IllegalStateException("Database is not opened. Call open() first.");
        }
        return conn;
    }

    // 연결 열기 (여러 번 호출해도 안전) + 폴더 생성 + PRAGMA 설정
    public Connection open() throws SQLException {
        if (!isOpen()) {
            conn = DriverManager.getConnection(URL);
            setSQLiteOptions(conn); // SQLite 권장 설정
        }
        return conn;
    }

    // 연결 닫기
    public void close() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
            conn = null;
        }
    }

    // SQLite PRAGMA
    private static void setSQLiteOptions(Connection c) throws SQLException {
    	try (Statement st = c.createStatement()) {
            // 외래키 제약 활성화 (기본 OFF)
            st.execute("PRAGMA foreign_keys = ON");
            // 동시 접근 시 대기 시간. 삽입/갱신 충돌 완화.
            st.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
            // 안정적 쓰기(로그 기반). 로컬 앱에 적합.
            st.execute("PRAGMA journal_mode = WAL");
            // 성능/안정성 밸런스
            st.execute("PRAGMA synchronous = NORMAL");
            // 임시 인덱스/테이블 메모리 사용
            st.execute("PRAGMA temp_store = MEMORY");
            // 페이지 캐시 (KB 단위; -8192 = 8MB)
            st.execute("PRAGMA cache_size = -8192");
            // 앱 식별자(선택): 'PSYS'
            st.execute("PRAGMA application_id = 0x50535953");
        }
    }

    // DB구조 (스키마)
    public void setup() throws SQLException {
        if (!isOpen()) open();
        
        boolean prevAuto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        
        try (Statement st = conn.createStatement()) {
        	
            // 1) users: 의료진/연구자/관리자/환자 계정
        	st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      username      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                      email         TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                      password_hash TEXT    NOT NULL,
                      role          TEXT    DEFAULT 'CLINICIAN'
                                   CHECK(role IN ('CLINICIAN','ADMIN','RESEARCHER','PATIENT')),
                      clinician_no  TEXT,   -- 의료인 번호(면허/사번 등), 비의료인 계정은 NULL
                      created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
        	
        	// 1-1) 의료인만 clinician_no 유일
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_users_clinician_no
                ON users(clinician_no) WHERE role='CLINICIAN'
            """);

            // 2) patients : 환자 PHI(식별자) 분리
            st.execute("""
                CREATE TABLE IF NOT EXISTS patients (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  mrn         TEXT,           -- 병원 환자번호(있으면 UNIQUE 변경)
                  name        TEXT,           -- 항시 본명 X
                  birth_date  TEXT,           -- ISO8601 형식 'YYYY-MM-DD' 권장
                  sex         TEXT CHECK(sex IN ('M','F')),
                  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // 3) care_assignments : 의료인 ↔ 환자 접근권한(다대다 매핑)
            st.execute("""
                CREATE TABLE IF NOT EXISTS care_assignments (
                  user_id      INTEGER NOT NULL,
                  patient_id   INTEGER NOT NULL,
                  role_in_care TEXT,  -- 'ATTENDING','RESIDENT','RN','RESEARCH' 등
                  assigned_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(user_id, patient_id),
                  FOREIGN KEY(user_id)    REFERENCES users(id)    ON DELETE CASCADE,
                  FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
                )
            """);
            
            
            // 4) encounters (선택): 환자의 진료/시뮬 회차 묶기
            st.execute("""
                CREATE TABLE IF NOT EXISTS encounters (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  patient_id  INTEGER NOT NULL,
                  type        TEXT,               -- 'OPD','IPD','SIM','ED' 등 자유롭게
                  started_at  TIMESTAMP,
                  ended_at    TIMESTAMP,
                  note        TEXT,
                  FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_enc_patient_time ON encounters(patient_id, started_at)");

            // 5) characters : 환자별 시뮬/스냅샷, 생성자 기록
            st.execute("""
                CREATE TABLE IF NOT EXISTS characters (
                  id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                  patient_id          INTEGER NOT NULL,     -- 대상 환자
                  created_by_user_id  INTEGER NOT NULL,     -- 생성자(의료인/사용자)
                  name                TEXT    NOT NULL,     -- 예: 'baseline','ICU-day1'
                  sex                 TEXT    CHECK(sex IN ('M','F')), -- 환자 sex 복사/가명
                  height_cm           REAL,
                  weight_kg           REAL,
                  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(patient_id)         REFERENCES patients(id) ON DELETE CASCADE,
                  FOREIGN KEY(created_by_user_id) REFERENCES users(id)    ON DELETE CASCADE
                )
            """);
            // 같은 환자 내 캐릭터명 중복 방지 → 조회/관리 편의성
            st.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_char_patient_name
                ON characters(patient_id, name)
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_char_patient ON characters(patient_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_char_creator ON characters(created_by_user_id)");

            // 6) vitals : 캐릭터별 시계열 측정치 (필요 시 컬럼 확장)
            st.execute("""
                CREATE TABLE IF NOT EXISTS vitals (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  character_id  INTEGER NOT NULL,
                  hr        INTEGER,   -- bpm
                  sbp       REAL,      -- Systolic BP, mmHg
                  dbp       REAL,      -- Diastolic BP, mmHg
                  map       REAL,      -- mmHg
                  rr        INTEGER,   -- breaths/min
                  spo2      REAL,      -- %
                  glucose   REAL,      -- mg/dL
                  temp      REAL,      -- °C
                  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(character_id) REFERENCES characters(id) ON DELETE CASCADE,
                  CHECK (hr      IS NULL OR hr BETWEEN 20 AND 260),
                  CHECK (rr      IS NULL OR rr BETWEEN 2  AND 80),
                  CHECK (spo2    IS NULL OR spo2 BETWEEN 0  AND 100),
                  CHECK (temp    IS NULL OR temp BETWEEN 25.0 AND 45.0)
                )
            """);
            
            // 시계열 조회 최적화
            st.execute("CREATE INDEX IF NOT EXISTS idx_vitals_char ON vitals(character_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_vitals_time ON vitals(recorded_at)");
            
            st.execute("""
                   CREATE TABLE IF NOT EXISTS schema_version(
            			version INTEGER NOT NULL,
            			applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            		)
                """);

            conn.commit();
        } catch (SQLException e) {
            conn.rollback(); // 오류 시 롤백
            throw e;
        } finally {
            // 트랜잭션 묶기
            if (conn != null) {
                try { conn.setAutoCommit(prevAuto); } 
                catch (SQLException ignored) {}
            }
        }
    }
}
