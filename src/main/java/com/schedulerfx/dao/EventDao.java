package com.schedulerfx.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.schedulerfx.model.t2_schedule;

public interface EventDao {
	
	t2_schedule insert(t2_schedule e);
    void delete(long id);
    void update(t2_schedule e);
    
    List<t2_schedule> findByDate(LocalDate date);
    List<t2_schedule> findByMonth(YearMonth month);
    
    // 시각화용 카테고리
    Map<String, Integer> getCategoryStats();
    
    Map<String, Integer> getCategoryStatsByDate(LocalDateTime startDate, LocalDateTime now);
}