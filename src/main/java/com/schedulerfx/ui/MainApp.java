package com.schedulerfx.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.schedulerfx.dao.EventDao;
import com.schedulerfx.dao.SqliteEventDao;
import com.schedulerfx.model.Event;
import com.schedulerfx.service.NotificationService;

public class MainApp extends Application {

    private YearMonth currentMonth = YearMonth.now();
    private final EventDao eventDao = new SqliteEventDao("scheduler.db");
    private final NotificationService notifier = new NotificationService();

    private CalendarView calendarView;
    private Label monthLabel;
    private Spinner<Integer> yearSpinner;
    private Spinner<Integer> monthSpinner;

    @Override
    public void start(Stage stage) {
        monthLabel = new Label();
        Button prev = new Button("◀");
        Button next = new Button("▶");

        prev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); refresh(); });
        next.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); refresh(); });

        yearSpinner = new Spinner<>(1900, 2100, currentMonth.getYear());
        monthSpinner = new Spinner<>(1, 12, currentMonth.getMonthValue());
        yearSpinner.setEditable(true);
        monthSpinner.setEditable(true);

        Button go = new Button("이동");
        go.setOnAction(e -> {
            int y = yearSpinner.getValue();
            int m = monthSpinner.getValue();
            currentMonth = YearMonth.of(y, m);
            refresh();
        });
        
        HBox top = new HBox(10, prev, monthLabel, next,  
                new Label("연:"), yearSpinner,
                new Label("월:"), monthSpinner,
                go);
        top.setPadding(new Insets(10));

        calendarView = new CalendarView(
        	    // 새 일정 등록
        	    date -> {
        	        EventDialog dialog = new EventDialog(stage, date);
        	        dialog.showAndWait().ifPresent(newEvent -> {
        	            Event saved = eventDao.insert(newEvent);
        	            notifier.schedule(saved);
        	            refresh();
        	        });
        	    },
        	    // 일정 수정
        	    event -> {
        	        LocalDate d = event.getStartAt().toLocalDate();
        	        EventDialog dialog = new EventDialog(stage, d, event);
        	        dialog.showAndWait().ifPresent(updated -> {
        	            notifier.cancel(event.getId());
        	            eventDao.update(updated);
        	            notifier.schedule(updated);
        	            refresh();
        	        });
        	    },
        	    // 일정 삭제
        	    event -> {
        	        notifier.cancel(event.getId());
        	        eventDao.delete(event.getId());
        	        refresh();
        	    }
        	);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(calendarView);

        refresh();

        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.setTitle("일정 달력");
        stage.show();

        // 앱 시작 시 이번 달 일정 알림 재등록
        List<Event> events = eventDao.findByMonth(currentMonth);
        events.forEach(notifier::schedule);
    }

    private void refresh() {
        monthLabel.setText(currentMonth.getYear() + "년 " + currentMonth.getMonthValue() + "월");
        if (yearSpinner != null) {
            yearSpinner.getValueFactory().setValue(currentMonth.getYear());
            monthSpinner.getValueFactory().setValue(currentMonth.getMonthValue());
        }
        calendarView.setMonth(currentMonth, eventDao.findByMonth(currentMonth));
    }

    @Override
    public void stop() {
        notifier.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
