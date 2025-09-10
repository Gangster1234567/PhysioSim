package physiosim.db;

import java.sql.*;
import java.util.*;

// 계정(users) 테이블 관리
public class UserRepository {

    private final Connection conn;

    public UserRepository(Connection conn) {
        this.conn = conn;
    }

    // 회원가입: (UNIQUE 위반 시 SQLException 터짐)
    public int register(String username, String email, String plainPassword) throws SQLException {
        if (username == null || email == null || plainPassword == null) throw new IllegalArgumentException("null");
        username = username.trim();
        email = email.trim();
        if (username.isEmpty() || email.isEmpty() || plainPassword.isEmpty()) throw new IllegalArgumentException("blank");

        final String sql = "INSERT INTO users(username, email, password_hash) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, Passwords.hash(plainPassword));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    // 로그인: 성공 시 user_id 반환, 실패 시 -1
    public int login(String username, String plainPassword) throws SQLException {
        if (username == null || plainPassword == null) return -1;
        username = username.trim();
        if (username.isEmpty() || plainPassword.isEmpty()) return -1;
        final String sql = "SELECT id, password_hash FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return -1;
                return Passwords.verify(plainPassword, rs.getString("password_hash")) ? rs.getInt("id") : -1;
            }
        }
    }

    // username == ID 중복 여부
    public boolean existsByUsername(String username) throws SQLException {
        final String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // email 중복 여부
    public boolean existsByEmail(String email) throws SQLException {
        final String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // 단건 조회(id)
    public Optional<User> findById(int id) throws SQLException {
        final String sql = """
            SELECT id, username, email, password_hash, created_at
              FROM users
             WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    // 단건 조회(username)
    public Optional<User> findByUsername(String username) throws SQLException {
        final String sql = """
            SELECT id, username, email, password_hash, created_at
              FROM users
             WHERE username = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    // 전체 목록
    public List<User> listAll() throws SQLException {
        final String sql = """
            SELECT id, username, email, password_hash, created_at
              FROM users
             ORDER BY created_at DESC, id DESC
        """;
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    // 계정 삭제
    public boolean delete(int id) throws SQLException {
        final String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    
    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("created_at")
        );
    }

    // DTO: 데이터 전달 객체 (로직 관리)
    public static class User {
        private final int id;
        private final String username;
        private final String email;
        private final String passwordHash;
        private final String createdAt;

        public User(int id, String username, String email, String passwordHash, String createdAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
            this.createdAt = createdAt;
        }
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }
        public String getCreatedAt() { return createdAt; }

        @Override public String toString() {
            return "User{id=" + id + ", username='" + username + "', email='" + email + "', createdAt='" + createdAt + "'}";
        }
    }
}

