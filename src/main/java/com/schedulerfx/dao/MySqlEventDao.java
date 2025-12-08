package com.schedulerfx.dao;

import com.schedulerfx.model.t2_schedule;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

public class MySqlEventDao implements EventDao {

    private static final String URL =
            "jdbc:mysql://nsyun.synology.me:3306/db"
                    + "?serverTimezone=Asia/Seoul"
                    + "&useSSL=false"
                    + "&characterEncoding=UTF-8";

    private static final String USER = "user";
    private static final String PASSWORD = "user1234";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC 드라이버를 찾을 수 없습니다.", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public MySqlEventDao() {
        initTable();
    }

    private void initTable() {
        String sql ="CREATE TABLE IF NOT EXISTS t2_schedule ("+
                "id BIGINT PRIMARY KEY AUTO_INCREMENT,"+
                "title VARCHAR(255) NOT NULL,"+
                "start_at DATETIME NOT NULL,"+
                "end_at DATETIME NULL,"+
                "category VARCHAR(50) NULL,"+
                "remind_minutes INT NOT NULL"+
                ")";
            

        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("t2_schedule 테이블 생성 실패", e);
        }
    }

    @Override
    public t2_schedule insert(t2_schedule e) {
        String sql = "INSERT INTO t2_schedule " +
                "(title, start_at, end_at, category, remind_minutes) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getTitle());
            ps.setTimestamp(2, Timestamp.valueOf(e.getStartAt()));

            if (e.getEndAt() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(e.getEndAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }

            ps.setString(4, e.getCategory());
            ps.setInt(5, e.getRemindMinutes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    e.setId(id);
                }
            }

            return e;
        } catch (SQLException ex) {
            throw new RuntimeException("t2_schedule INSERT 실패", ex);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM t2_schedule WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("t2_schedule DELETE 실패", ex);
        }
    }

    @Override
    public void update(t2_schedule e) {
        if (e.getId() == null) {
            throw new IllegalArgumentException("id 없는 일정은 업데이트할 수 없습니다.");
        }

        String sql = "UPDATE t2_schedule " +
                "SET title = ?, start_at = ?, end_at = ?, category = ?, remind_minutes = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getTitle());
            ps.setTimestamp(2, Timestamp.valueOf(e.getStartAt()));

            if (e.getEndAt() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(e.getEndAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }

            ps.setString(4, e.getCategory());
            ps.setInt(5, e.getRemindMinutes());
            ps.setLong(6, e.getId());

            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("t2_schedule UPDATE 실패", ex);
        }
    }

    @Override
    public List findByDate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        String sql = "SELECT id, title, start_at, end_at, category, remind_minutes " +
                "FROM t2_schedule " +
                "WHERE start_at >= ? AND start_at < ? " +
                "ORDER BY start_at";

        return queryList(sql, from, to);
    }

    @Override
    public List findByMonth(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        String sql = "SELECT id, title, start_at, end_at, category, remind_minutes " +
                "FROM t2_schedule " +
                "WHERE start_at >= ? AND start_at < ? " +
                "ORDER BY start_at";

        return queryList(sql, from, to);
    }

    @Override
    public Map getCategoryStats() {
        String sql = "SELECT COALESCE(category, '미지정') AS cat, COUNT(*) AS cnt " +
                "FROM t2_schedule " +
                "GROUP BY cat";

        Map<String, Integer> result = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String cat = rs.getString("cat");
                int cnt = rs.getInt("cnt");
                result.put(cat, cnt);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("카테고리 통계 조회 실패", ex);
        }

        return result;
    }

    private List<t2_schedule> queryList(String sql,
                                        LocalDateTime from,
                                        LocalDateTime to) {
        List<t2_schedule> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String title = rs.getString("title");
                    LocalDateTime startAt = rs.getTimestamp("start_at").toLocalDateTime();

                    Timestamp endTs = rs.getTimestamp("end_at");
                    LocalDateTime endAt = (endTs != null) ? endTs.toLocalDateTime() : null;

                    String category = rs.getString("category");
                    int remind = rs.getInt("remind_minutes");

                    t2_schedule e = new t2_schedule(id, title, startAt, endAt, category, remind);
                    list.add(e);
                }
            }

        } catch (SQLException ex) {
            throw new RuntimeException("t2_schedule 목록 조회 실패", ex);
        }

        return (List) list;
    }
}
