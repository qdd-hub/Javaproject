package com.schedulerfx.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter; // 날짜 포맷터 추가
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.schedulerfx.model.t2_schedule; // 모델 클래스 import

// 클래스 이름을 MySqlEventDao로 변경 추천
public class MySqlEventDao implements EventDao {
    
    // MySQL 접속 정보
	private static final String DB_URL = "jdbc:mysql://nsyun.synology.me:3306/db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
    private static final String DB_USER = "user"; 
    private static final String DB_PASSWORD = "user1234";

    public MySqlEventDao() {
        init();
    }

    private void init() {
        String sql = "CREATE TABLE IF NOT EXISTS t2_schedule (" +
                      "id BIGINT PRIMARY KEY AUTO_INCREMENT, "+ 
                      "title VARCHAR(255) NOT NULL, "+         
                      "start_at DATETIME NOT NULL, "+           
                      "end_at DATETIME, "+
                      "category VARCHAR(50), " +
                      "remind_minutes INTEGER NOT NULL"+
                      ");";
        
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException("테이블 생성 실패: " + ex.getMessage(), ex);
        }
    }

    // 공통 연결 메소드 분리
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    @Override
    public t2_schedule insert(t2_schedule e) {
        String sql = "INSERT INTO t2_schedule(title, start_at, end_at, category, remind_minutes) VALUES(?,?,?,?,?)";
        
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getTitle());
            ps.setObject(2, e.getStartAt()); // MySQL이 LocalDateTime을 알아서 변환해줌
            ps.setObject(3, e.getEndAt());
            ps.setString(4, e.getCategory());
            ps.setInt(5, e.getRemindMinutes());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    e.setId(rs.getLong(1));
            }
            return e;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public void update(t2_schedule e) {
        if (e.getId() == null) {
            throw new IllegalArgumentException("id 없는 이벤트는 update 불가");
        }

        String sql = "UPDATE t2_schedule SET title=?, start_at=?, end_at=?, category=?, remind_minutes=? WHERE id=?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getTitle());
            ps.setObject(2, e.getStartAt());
            ps.setObject(3, e.getEndAt());
            ps.setString(4, e.getCategory());
            ps.setInt(5, e.getRemindMinutes());
            ps.setLong(6, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void delete(long id) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM t2_schedule WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<t2_schedule> findByDate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        String sql = "SELECT * FROM t2_schedule WHERE start_at >= ? AND start_at < ? ORDER BY start_at";
        return query(sql, from, to);
    }

    @Override
    public List<t2_schedule> findByMonth(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        String sql = "SELECT * FROM t2_schedule WHERE start_at >= ? AND start_at < ? ORDER BY start_at";
        return query(sql, from, to);
    }

    // 쿼리 파라미터를 LocalDateTime으로 받도록 수정
    private List<t2_schedule> query(String sql, LocalDateTime from, LocalDateTime to) {
        List<t2_schedule> list = new ArrayList<>();
        try (Connection c = getConnection(); 
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, from);
            ps.setObject(2, to);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String title = rs.getString("title");
                    
                    LocalDateTime startAt = rs.getObject("start_at", LocalDateTime.class);
                    LocalDateTime endAt = rs.getObject("end_at", LocalDateTime.class);
                    
                    String category = rs.getString("category");
                    int remind = rs.getInt("remind_minutes");
                    
                    list.add(new t2_schedule(id, title, startAt, endAt, category, remind));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    // 전체 통계
    @Override
    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT category, COUNT(*) as count FROM t2_schedule GROUP BY category";
        try (Connection c = getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                String category = rs.getString("category");
                int count = rs.getInt("count");
                
                if(category == null || category.isEmpty()) category = "미지정";
                stats.put(category, count);
            }
            
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        
        return stats;
    }
    
    // 기간 별 통계
    @Override
    public Map<String, Integer> getCategoryStatsByDate(LocalDateTime startDate) {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT category, COUNT(*) as count FROM t2_schedule " +
                     "WHERE start_at >= ? " + 
                     "GROUP BY category";

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, startDate);

            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    String category = rs.getString("category");
                    int count = rs.getInt("count");
                    
                    if(category == null || category.isEmpty()) category = "미지정";
                    stats.put(category, count);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return stats;
    }
}