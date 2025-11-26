package com.schedulerfx.dao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.schedulerfx.model.Event;

public interface EventDao {
    Event insert(Event e);
    void delete(long id);
    void update(Event e);
    List<Event> findByDate(LocalDate date);
    List<Event> findByMonth(YearMonth month);
}