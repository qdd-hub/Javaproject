package com.schedulerfx.ui;

import com.schedulerfx.model.t2_schedule;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CalendarView extends GridPane {

    private final Consumer<LocalDate> onAddRequest;
    private final Consumer<t2_schedule> onEditRequest;
    private final Consumer<t2_schedule> onDeleteRequest;

    public CalendarView(Consumer<LocalDate> onAddRequest,
                        Consumer<t2_schedule> onEditRequest,
                        Consumer<t2_schedule> onDeleteRequest) {
        this.onAddRequest = onAddRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;

        setHgap(2);
        setVgap(2);
        setPadding(new Insets(5));
    }

    public void setMonth(YearMonth month, List<t2_schedule> events) {
        getChildren().clear();

        // 날짜별로 이벤트 묶기
        Map<LocalDate, List<t2_schedule>> byDate =
                events.stream()
                      .collect(Collectors.groupingBy(e -> e.getStartAt().toLocalDate()));

        LocalDate first = month.atDay(1);
        int firstDayOfWeek = first.getDayOfWeek().getValue(); // Mon=1 ... Sun=7
        int startCol = firstDayOfWeek % 7;                     // Sun=0 ... Sat=6

        LocalDate cursor = first.minusDays(startCol); // 달력 시작 칸에 올 날짜

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                LocalDate date = cursor;
                List<t2_schedule> dayEvents = byDate.getOrDefault(date, List.of());

                DayCell cell = new DayCell(
                        date,
                        month,
                        dayEvents,
                        onAddRequest,
                        onEditRequest,
                        onDeleteRequest
                );

                add(cell, col, row);
                cursor = cursor.plusDays(1);
            }
        }
    }
}