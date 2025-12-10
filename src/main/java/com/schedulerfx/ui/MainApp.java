package com.schedulerfx.ui;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.schedulerfx.dao.EventDao;
import com.schedulerfx.dao.MySqlEventDao;
import com.schedulerfx.model.t2_schedule;
import com.schedulerfx.service.NotificationService;

public class MainApp extends Application {

    private YearMonth currentMonth = YearMonth.now();
    private final EventDao eventDao = new MySqlEventDao();
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

        setupIntegerSpinner(monthSpinner, 1, 12, true, false);
        setupIntegerSpinner(yearSpinner, 1900, 2100, false, true);

        Button go = new Button("이동");
        go.setOnAction(e -> {
            int y = yearSpinner.getValue();
            int m = monthSpinner.getValue();
            currentMonth = YearMonth.of(y, m);
            refresh();
        });
        
        // 카테고리 뷰
        Button statsBtn = new Button("통계");
        statsBtn.setOnAction(e -> {

            Stage statsStage = new Stage();
            statsStage.setTitle("카테고리 통계");
            
            Scene statsScene = new Scene(new StatsView(), 600, 500);
            statsStage.setScene(statsScene);
            
            statsStage.initOwner(stage);
            
            statsStage.show();
        });
        
        HBox top = new HBox(10, prev, monthLabel, next,  
                new Label("연:"), yearSpinner,
                new Label("월:"), monthSpinner,
                go, statsBtn);
        top.setPadding(new Insets(10));

        calendarView = new CalendarView(
        	    // 새 일정 등록
        	    date -> {
        	        EventDialog dialog = new EventDialog(stage, date);
        	        dialog.showAndWait().ifPresent(newEvent -> {
        	        	t2_schedule saved = eventDao.insert(newEvent);
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
        List<t2_schedule> events = eventDao.findByMonth(currentMonth);
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
    
    private void setupIntegerSpinner(
            Spinner<Integer> spinner,
            int min,
            int max,
            boolean checkRangeOnChange, // true: 입력 시점 범위 체크(시간/월)
            boolean fixOnFocusLost      // true: 포커스 잃을 때 범위 밖이면 lastValid로 롤백(연도)
    ) {
        SpinnerValueFactory<Integer> vf = spinner.getValueFactory();
        StringConverter<Integer> converter = vf.getConverter();

        // 마지막으로 유효했던 값 기억
        IntegerProperty lastValid = new SimpleIntegerProperty(vf.getValue());

        TextFormatter<Integer> formatter = new TextFormatter<>(
                converter,
                vf.getValue(),
                change -> {
                    if (!change.isContentChange()) return change;

                    String newText = change.getControlNewText();
                    if (newText.isEmpty()) {
                        // 비워두는 건 허용
                        return change;
                    }

                    // 숫자만 허용
                    if (!newText.matches("\\d*")) {
                        return null; // 문자면 입력 무시 > 기존 값 유지
                    }

                    // 여기서 바로 범위 체크할지 여부
                    if (checkRangeOnChange) {
                        // 시간/월처럼 “입력 시점”에 바로 범위 체크
                        try {
                            int value = Integer.parseInt(newText);
                            if (value < min || value > max) {
                                return null; // 범위 밖이면 입력 취소 > 롤백
                            }
                            // 범위 안이면 마지막 정상값 갱신
                            lastValid.set(value);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    } else {
                        // 연도 모드: 숫자인지만 확인, 범위는 나중에 (포커스 아웃 시) 본격 체크
                        try {
                            int value = Integer.parseInt(newText);
                            if (value >= min && value <= max) {
                                // 범위 안인 숫자가 만들어졌으면 그때는 lastValid 갱신
                                lastValid.set(value);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    return change;
                }
        );

        spinner.getEditor().setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(vf.valueProperty());

        if (fixOnFocusLost) {
            // 연도 모드용: 포커스 잃을 때 최종 값 검사
            spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    String text = spinner.getEditor().getText();
                    if (text == null || text.isEmpty()) {
                        // 비어 있으면 마지막 정상값으로 롤백
                        int v = lastValid.get();
                        vf.setValue(v);
                        spinner.getEditor().setText(Integer.toString(v));
                    } else {
                        try {
                            int val = Integer.parseInt(text);
                            if (val < min || val > max) {
                                // 범위 밖 > 마지막 정상값으로 롤백
                                int v = lastValid.get();
                                vf.setValue(v);
                                spinner.getEditor().setText(Integer.toString(v));
                            } else {
                                // 범위 안 > 이 값을 새로운 정상값으로 채택
                                lastValid.set(val);
                                vf.setValue(val);
                                spinner.getEditor().setText(Integer.toString(val));
                            }
                        } catch (NumberFormatException e) {
                            int v = lastValid.get();
                            vf.setValue(v);
                            spinner.getEditor().setText(Integer.toString(v));
                        }
                    }
                }
            });
        }
    }



    @Override
    public void stop() {
        notifier.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}