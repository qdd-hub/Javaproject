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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.schedulerfx.model.Event;

public class SqliteEventDao implements EventDao {
	private final String url;

	public SqliteEventDao(String dbFilePath) {
		this.url = "jdbc:sqlite:" + dbFilePath;
		init();
	}

	private void init() {
		String sql = "CREATE TABLE IF NOT EXISTS event (" +
				      "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
				      "title TEXT NOT NULL, "+
				      "start_at TEXT NOT NULL, "+
				      "end_at TEXT, "+
				      "category TEXT, " +
				      "remind_minutes INTEGER NOT NULL"+
				      ");";
		try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
			s.execute(sql);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public Event insert(Event e) {
		String sql = "INSERT INTO event(title, start_at, end_at, category, remind_minutes) VALUES(?,?,?,?,?)";
		try (Connection c = DriverManager.getConnection(url);
				PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setString(1, e.getTitle());
			ps.setString(2, e.getStartAt().toString());
			ps.setString(3, e.getEndAt() == null ? null : e.getEndAt().toString());
			ps.setString(4,e.getCategory());
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
	public void update(Event e) {
	    if (e.getId() == null) {
	        throw new IllegalArgumentException("id 없는 이벤트는 update 불가");
	    }

	    String sql = "UPDATE event SET title=?, start_at=?, end_at=?, category=?, remind_minutes=? WHERE id=?";
	    try (Connection c = DriverManager.getConnection(url);
	         PreparedStatement ps = c.prepareStatement(sql)) {

	        ps.setString(1, e.getTitle());
	        ps.setString(2, e.getStartAt().toString());
	        ps.setString(3, e.getEndAt() == null ? null : e.getEndAt().toString());
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
		try (Connection c = DriverManager.getConnection(url);
				PreparedStatement ps = c.prepareStatement("DELETE FROM event WHERE id=?")) {
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public List<Event> findByDate(LocalDate date) {
		LocalDateTime from = date.atStartOfDay();
		LocalDateTime to = date.plusDays(1).atStartOfDay();
		String sql = "SELECT * FROM event WHERE start_at >= ? AND start_at < ? ORDER BY start_at";
		return query(sql, from.toString(), to.toString());
	}

	@Override
	public List<Event> findByMonth(YearMonth month) {
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
		String sql = "SELECT * FROM event WHERE start_at >= ? AND start_at < ? ORDER BY start_at";
		return query(sql, from.toString(), to.toString());
	}

	private List<Event> query(String sql, String a, String b) {
		List<Event> list = new ArrayList<>();
		try (Connection c = DriverManager.getConnection(url); 
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, a);
			ps.setString(2, b);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					long id = rs.getLong("id");
					String title = rs.getString("title");
					LocalDateTime startAt = LocalDateTime.parse(rs.getString("start_at"));
					
					String endStr = rs.getString("end_at");
					LocalDateTime endAt = endStr == null ? null : LocalDateTime.parse(endStr);
					
					String category = rs.getString("category");
					int remind = rs.getInt("remind_minutes");
					
					list.add(new Event(id, title, startAt, endAt, category, remind));
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
		return list;
	}

	@Override
	public Map<String, Integer> getCategoryStats() {
		Map<String, Integer> stats = new HashMap<>();
		String sql = "SELECT category, COUNT(*) as count FROM event GROUP BY category";
        try (Connection c = DriverManager.getConnection(url);
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
	
	
	
}
