package physiosim.db;

import java.sql.*;
import java.util.*;

// TABLE 관리!
public class CharacterRepository {
    private final Connection conn;

    public CharacterRepository(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "conn is null");
    }

    // 캐릭터 등록
    public int insert(int patientId, int createdByUserId,
                      String name, String sex, Double heightCm, Double weightKg) throws SQLException {
        final String sql = """
            INSERT INTO characters(patient_id, created_by_user_id, name, sex, height_cm, weight_kg)
            VALUES (?,?,?,?,?,?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, createdByUserId);
            ps.setString(3, name);
            if (sex == null) ps.setNull(4, Types.VARCHAR);
            else ps.setString(4, sex);
            if (heightCm == null) ps.setNull(5, Types.REAL);
            else ps.setDouble(5, heightCm);
            if (weightKg == null) ps.setNull(6, Types.REAL);
            else ps.setDouble(6, weightKg);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // 캐릭터 조회 (하나)
    public CharacterRow findById(int id) throws SQLException {
        final String sql = """
            SELECT id, patient_id, created_by_user_id, name, sex, height_cm, weight_kg, created_at
              FROM characters
             WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }
    // 특정 환자의 모든 캐릭터: 특이 케이스
    public List<CharacterRow> findByPatient(int patientId) throws SQLException {
        final String sql = """
            SELECT id, patient_id, created_by_user_id, name, sex, height_cm, weight_kg, created_at
              FROM characters
             WHERE patient_id = ?
             ORDER BY created_at
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CharacterRow> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    // 캐릭터 삭제
    public boolean delete(int id) throws SQLException {
        final String sql = "DELETE FROM characters WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // 매퍼
    private static CharacterRow mapRow(ResultSet rs) throws SQLException {
        return new CharacterRow(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getInt("created_by_user_id"),
                rs.getString("name"),
                rs.getString("sex"),
                rs.getObject("height_cm", Double.class), 
                rs.getObject("weight_kg", Double.class),
                rs.getString("created_at")
        );
    }

    // DTO
    public static record CharacterRow(
    		int id,
            int patientId,
            int createdByUserId,
            String name,
            String sex,
            Double heightCm,
            Double weightKg,
            String createdAt
    ) {}
}
