// User 테이블 관리
package physiosim.db;

import java.sql.*;
import java.util.*;

public class UserRepository {

	// 재사용할 연결
	private final Connection conn;

	public UserRepository(Connection conn) {
        this.conn = conn;
	}
	
	// 테이블 자동 생성
	public void init() throws SQLException {
        final String sql = """
            CREATE TABLE IF NOT EXISTS users (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              sex  TEXT CHECK(sex IN ('M','F')),
              height_cm REAL,
              weight_kg REAL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }
	
	// 새 사용자 저장하고 생성된 id 반환
    public int insert(String sex, double heightCm, double weightKg) throws SQLException {
        final String sql = "INSERT INTO users(sex, height_cm, weight_kg) VALUES(?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sex);
            ps.setDouble(2, heightCm);
            ps.setDouble(3, weightKg);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1; // 새로 생성된 id 반환
            }
        }
    }
	
    // 이어하기: id로 단건 조회 (없으면 Optional.empty())
    public Optional<User> findById(int id) throws SQLException {
        final String sql = """
            SELECT id, sex, height_cm, weight_kg, created_at
              FROM users
             WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }
    
    // 캐릭터 선택 목록: 전체 조회 (id 오름차순)
    public List<User> listAll() throws SQLException {
        final String sql = """
            SELECT id, sex, height_cm, weight_kg, created_at
              FROM users
             ORDER BY id ASC
            """;
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // 슬롯 삭제: 해당 id 행 삭제 (삭제되면 true)
    public boolean delete(int id) throws SQLException {
        final String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // 공통: ResultSet → User 매핑
    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("sex"),
            rs.getDouble("height_cm"),
            rs.getDouble("weight_kg"),
            rs.getString("created_at")
        );
    }

    // 조회 결과 전달용 DTO (불변 record)
    public record User(
        int id,
        String sex,       // 'M' or 'F'
        double heightCm,  // cm
        double weightKg,  // kg
        String createdAt  // 'YYYY-MM-DD HH:MM:SS'
    ) {}
}
