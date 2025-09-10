// DB 연결 관리
package physiosim.db;

import java.sql.*;

public class Database {
    
	// DB 파일 경로 (상수로 고정): 캡슐화(정보 은닉) + 상수화(값 고정)
    private static final String URL = "jdbc:sqlite:data/app.db";
    private Connection conn;
    
    // 예외처리
    public Connection getConnection() {
        if (conn == null) {
            throw new IllegalStateException("Database is not opened. Call open() first.");
        }
        return conn;
    }

    // open() 메서드: 연결 열기
    public Connection open() throws SQLException {
         if (conn == null || conn.isClosed()) {
             conn = DriverManager.getConnection(URL);
         }
    	return conn;	
    }
    
    // close() 메서드: 연결 닫기
    public void close() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
            conn = null;
        }
    }

}
