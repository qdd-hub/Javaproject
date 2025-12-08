package com.schedulerfx.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.schedulerfx.model.t2_schedule;

public class EventDialog extends Dialog<t2_schedule> {

    private final LocalDate baseDate;
    private final t2_schedule original; // null이면 새 일정, 아니면 수정

    public EventDialog(Stage owner, LocalDate defaultDate) {
        this(owner, defaultDate, null);
    }

    public EventDialog(Stage owner, LocalDate defaultDate, t2_schedule original) {
        this.baseDate = defaultDate;
        this.original = original;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(original == null ? "일정 등록" : "일정 수정");

        Label dateLabel = new Label("날짜: " + baseDate.toString());

        LocalTime now = LocalTime.now()
                .withSecond(0)
                .withNano(0);
        LocalTime defaultEnd = now.plusHours(1);

        int shInit;
        int smInit;
        int ehInit;
        int emInit;
        boolean hasEnd = false;

        if (original != null) {
            // 수정 모드: 기존 값 사용
            shInit = original.getStartAt().getHour();
            smInit = original.getStartAt().getMinute();

            if (original.getEndAt() != null) {
                hasEnd = true;
                ehInit = original.getEndAt().getHour();
                emInit = original.getEndAt().getMinute();
            } else {
                LocalTime t = original.getStartAt().toLocalTime().plusHours(1);
                ehInit = t.getHour();
                emInit = t.getMinute();
            }
        } else {
            // 새 일정: 시작=현재, 종료=+1시간
            shInit = now.getHour();
            smInit = now.getMinute();
            ehInit = defaultEnd.getHour();
            emInit = defaultEnd.getMinute();

            // 기본은 종료시간 미사용으로 둠 (체크하면 활성화)
            hasEnd = false;
        }
        
        Spinner<Integer> sh = new Spinner<>(0,23, shInit);
        Spinner<Integer> sm = new Spinner<>(0,59, smInit);
        Spinner<Integer> eh = new Spinner<>(0, 23, ehInit);
        Spinner<Integer> em = new Spinner<>(0, 59, emInit);
        sh.setEditable(true);
        sm.setEditable(true);
        eh.setEditable(true);
        em.setEditable(true);
        setupIntegerSpinner(sh, 0, 23, true, false);
        setupIntegerSpinner(sm, 0, 59, true, false);
        setupIntegerSpinner(eh, 0, 23, true, false);
        setupIntegerSpinner(em, 0, 59, true, false);
        
        HBox startBox = new HBox(5, sh, new Label(":"), sm);
        HBox endBox   = new HBox(5, eh, new Label(":"), em);
        
        // 종료시간 사용 여부 체크박스
        CheckBox endTimeCheck = new CheckBox("종료시간 사용");
        endTimeCheck.setSelected(hasEnd);
        eh.setDisable(!hasEnd);
        em.setDisable(!hasEnd);
        
        endTimeCheck.selectedProperty().addListener((obs, oldV, newV) -> {
            eh.setDisable(!newV);
            em.setDisable(!newV);
        });

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
        grid.add(new Label("시작 시간"), 0, row);
        grid.add(startBox, 1, row); row++;
        
        HBox endRow = new HBox(10, endBox, endTimeCheck);
        grid.add(new Label("종료 시간"), 0, row);
        grid.add(endRow, 1, row); row++;
        
        grid.add(new Label("카테고리"), 0, row);
        grid.add(categoryCombo, 1, row); row++;
        
        grid.add(new Label("내용"), 0, row);
        grid.add(title, 1, row); row++;
        
        grid.add(new Label("알림"), 0, row);
        grid.add(remindBox, 1, row);

        getDialogPane().setContent(grid);

        ButtonType ok = new ButtonType(original == null ? "등록" : "수정", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        
        Button okButton = (Button) getDialogPane().lookupButton(ok);
        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            String text = title.getText().trim();
            if (text.isEmpty()) {
                showWarn("내용을 입력해 주세요.");
                e.consume();
                return;
            }

            if (endTimeCheck.isSelected()) {
                LocalTime startTime = LocalTime.of(sh.getValue(), sm.getValue());
                LocalTime endTime   = LocalTime.of(eh.getValue(), em.getValue());
                // 종료가 시작보다 같거나 빠르면 오류
                if (!endTime.isAfter(startTime)) {
                    showWarn("종료 시간은 시작 시간보다 늦어야 합니다.");
                    e.consume();
                    return;
                }
            }
        });

        setResultConverter(bt -> {
            if (bt != ok) return null;

            String text = title.getText().trim();
            if (text.isEmpty()) return null;
            
            Toggle sel = remindGroup.getSelectedToggle();
            if (sel == null) return null;

            int remind = (int)sel.getUserData();
            LocalTime startTime = LocalTime.of(sh.getValue(), sm.getValue());
            LocalDateTime startAt = baseDate.atTime(startTime);
            
            LocalDateTime endAt = null;
            if (endTimeCheck.isSelected()) {
                LocalTime endTime = LocalTime.of(eh.getValue(), em.getValue());
                endAt = baseDate.atTime(endTime);
            }

            
            String category = categoryCombo.getValue();

            t2_schedule result = new t2_schedule(text, startAt, null, category,remind);
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
    
    private void showWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("입력 오류");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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
                        // 시간/월처럼 바로 범위 체크
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
            // 연도 모드용> 포커스 잃을 때 최종 값 검사
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

}