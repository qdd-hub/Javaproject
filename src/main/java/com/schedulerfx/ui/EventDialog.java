package com.schedulerfx.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.schedulerfx.model.Event;

public class EventDialog extends Dialog<Event> {

    private final LocalDate baseDate;
    private final Event original; // null이면 새 일정, 아니면 수정

    public EventDialog(Stage owner, LocalDate defaultDate) {
        this(owner, defaultDate, null);
    }

    public EventDialog(Stage owner, LocalDate defaultDate, Event original) {
        this.baseDate = defaultDate;
        this.original = original;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(original == null ? "일정 등록" : "일정 수정");

        Label dateLabel = new Label("날짜: " + baseDate.toString());

        // 시간: 기존 값 있으면 그걸로, 없으면 17:00 같은 기본값 시간 설정
        int initHour = 17;
        int initMin = 0;
        if (original != null) {
            initHour = original.getStartAt().getHour();
            initMin = original.getStartAt().getMinute();
        }
        Spinner<Integer> sh = new Spinner<>(0,23, initHour);
        Spinner<Integer> sm = new Spinner<>(0,59, initMin);
        sh.setEditable(true);
        sm.setEditable(true);
        HBox timeBox = new HBox(5, sh, new Label(":"), sm);

        TextField title = new TextField();
        title.setPromptText("예: 17:00 영화 보기");
        if (original != null) {
            title.setText(original.getTitle());
        }

        ToggleGroup remindGroup = new ToggleGroup();
        HBox remindBox = new HBox(5,
                remindToggle("1시간 전", 60, remindGroup),
                remindToggle("30분 전", 30, remindGroup),
                remindToggle("15분 전", 15, remindGroup),
                remindToggle("5분 전", 5, remindGroup)
        );
        
        // 카테고리 설정
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("업무", "개인", "학습", "운동", "기타");
        categoryCombo.getSelectionModel().select(0); // 기본값 "업무"
        if (original != null && original.getCategory() != null) {
            categoryCombo.getSelectionModel().select(original.getCategory());
        }

        // 기본/기존 알림 설정
        int defaultRemind = original != null ? original.getRemindMinutes() : 5;
        // 선택한게 없으면 첫번째 선택
        boolean isSelected = false;
        for (Toggle t : remindGroup.getToggles()) {
            if ((int) t.getUserData() == defaultRemind) {
            	t.setSelected(true);
            	isSelected = true;
                break;
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        int row = 0;
        grid.add(dateLabel, 0, row, 2, 1); row++;
        grid.add(new Label("시간"), 0, row);
        grid.add(timeBox, 1, row); row++;
        
        grid.add(new Label("카테고리"), 0, row);
        grid.add(categoryCombo, 1, row); row++;
        
        grid.add(new Label("내용"), 0, row);
        grid.add(title, 1, row); row++;
        
        grid.add(new Label("알림"), 0, row);
        grid.add(remindBox, 1, row);

        getDialogPane().setContent(grid);

        ButtonType ok = new ButtonType(original == null ? "등록" : "수정", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        setResultConverter(bt -> {
            if (bt != ok) return null;

            String text = title.getText().trim();
            if (text.isEmpty()) return null;

            int remind = (int) remindGroup.getSelectedToggle().getUserData();
            LocalTime t = LocalTime.of(sh.getValue(), sm.getValue());
            LocalDateTime startAt = baseDate.atTime(t);
            
            String category = categoryCombo.getValue();

            Event result = new Event(text, startAt, null, category,remind);
            if (original != null) {
                result.setId(original.getId()); // update용
            }
            return result;
        });
    }

    private RadioButton remindToggle(String text, int minutes, ToggleGroup tg) {
        RadioButton rb = new RadioButton(text);
        rb.setUserData(minutes);
        rb.setToggleGroup(tg);
        return rb;
    }
}