package physiosim.db;

import java.sql.*;
import java.util.*;

//TABLE 관리!
public class VitalRepository {
    private final Connection conn;

    public VitalRepository(Connection conn) {
        this.conn = Objects.requireNonNull(conn, "conn is null");
    }

    // 바이탈 기록 삽입 및 생성된 ID 반환
    public int insert(int characterId, Integer hr, Double sbp, Double dbp,
                      Double map, Integer rr, Double spo2, Double glucose, Double temp) throws SQLException {
        final String sql = """
            INSERT INTO vitals(character_id, hr, sbp, dbp, map, rr, spo2, glucose, temp)
            VALUES (?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, characterId);
            if (hr == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, hr);
            if (sbp == null) ps.setNull(3, Types.REAL); else ps.setDouble(3, sbp);
            if (dbp == null) ps.setNull(4, Types.REAL); else ps.setDouble(4, dbp);
            if (map == null) ps.setNull(5, Types.REAL); else ps.setDouble(5, map);
            if (rr == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, rr);
            if (spo2 == null) ps.setNull(7, Types.REAL); else ps.setDouble(7, spo2);
            if (glucose == null) ps.setNull(8, Types.REAL); else ps.setDouble(8, glucose);
            if (temp == null) ps.setNull(9, Types.REAL); else ps.setDouble(9, temp);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // 특정 캐릭터의 최신 기록 (하나)
    public VitalRow findLatestByCharacter(int characterId) throws SQLException {
        final String sql = """
            SELECT id, character_id, hr, sbp, dbp, map, rr, spo2, glucose, temp, recorded_at
              FROM vitals
             WHERE character_id = ?
             ORDER BY recorded_at DESC, id DESC
             LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // 특정 캐릭터의 모든 기록
    public List<VitalRow> findByCharacter(int characterId) throws SQLException {
        final String sql = """
            SELECT id, character_id, hr, sbp, dbp, map, rr, spo2, glucose, temp, recorded_at
              FROM vitals
             WHERE character_id = ?
             ORDER BY recorded_at
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                List<VitalRow> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    // 특정 캐릭터의 기록을 기간으로 조회
    public List<VitalRow> findByCharacterBetween(int characterId, String from, String to) throws SQLException {
        final String sql = """
            SELECT id, character_id, hr, sbp, dbp, map, rr, spo2, glucose, temp, recorded_at
              FROM vitals
             WHERE character_id = ?
               AND recorded_at BETWEEN ? AND ?
             ORDER BY recorded_at
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, characterId);
            ps.setString(2, from);
            ps.setString(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                List<VitalRow> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    // 매퍼
    private static VitalRow mapRow(ResultSet rs) throws SQLException {
        return new VitalRow(
                rs.getInt("id"),
                rs.getInt("character_id"),
                (Integer) rs.getObject("hr"),
                (Double) rs.getObject("sbp"),
                (Double) rs.getObject("dbp"),
                (Double) rs.getObject("map"),
                (Integer) rs.getObject("rr"),
                (Double) rs.getObject("spo2"),
                (Double) rs.getObject("glucose"),
                (Double) rs.getObject("temp"),
                rs.getString("recorded_at")
        );
    }

    // DTO
    public static record VitalRow(
            int id,
            int characterId,
            Integer hr,
            Double sbp,
            Double dbp,
            Double map,
            Integer rr,
            Double spo2,
            Double glucose,
            Double temp,
            String recordedAt
    ) {}
}
