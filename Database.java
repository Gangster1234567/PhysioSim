// DB 연결 관리
package physiosim.db;

import java.sql.*;

public class Database {

    //  DB 파일 경로 (상수로 고정): 캡슐화(정보 은닉) + 상수화(값 고정)
    private static final String DB_FILE = "data/app.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private Connection conn;

    // 현재 연결 상태 확인
    public boolean isOpen() {
        try { return conn != null && !conn.isClosed(); }
        catch (SQLException e) { return false; }
    }

    // 외부에서 커넥션 호출
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
            // 외래키 제약 활성화 (기본이 OFF라 꼭 켜야 함)
            st.execute("PRAGMA foreign_keys = ON");
            // 동시 접근 시 대기 시간. 삽입/갱신 충돌 완화.
            st.execute("PRAGMA busy_timeout = 5000");
            // 안정적 쓰기(로그 기반). 로컬 앱에 적합.
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    // 
    public void setup() throws SQLException {
        if (!isOpen()) open();
        try (Statement st = conn.createStatement()) {
            // users
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  username      TEXT    NOT NULL UNIQUE,
                  email         TEXT    NOT NULL UNIQUE,
                  password_hash TEXT    NOT NULL,
                  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // characters
            st.execute("""
                CREATE TABLE IF NOT EXISTS characters (
                  id         INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id    INTEGER NOT NULL,
                  name       TEXT    NOT NULL,
                  sex        TEXT    CHECK(sex IN ('M','F')),
                  height_cm  REAL,
                  weight_kg  REAL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_char_user ON characters(user_id)");

            // vitals
            st.execute("""
                CREATE TABLE IF NOT EXISTS vitals (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  character_id  INTEGER NOT NULL,
                  hr       REAL,   -- bpm
                  map      REAL,   -- mmHg
                  rr       REAL,   -- breaths/min
                  spo2     REAL,   -- %
                  glucose  REAL,   -- mg/dL
                  temp     REAL,   -- °C
                  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY(character_id) REFERENCES characters(id) ON DELETE CASCADE
                )
            """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_vitals_char_time
                ON vitals(character_id, recorded_at DESC)
            """);
        }
    }
}
