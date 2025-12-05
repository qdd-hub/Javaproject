package com.schedulerfx.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;

import com.schedulerfx.model.t2_schedule;

import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class DayCell extends VBox {

    private final LocalDate date;
    private final YearMonth shownMonth;
    private final Consumer<LocalDate> onAddRequest;
    private final Consumer<t2_schedule> onEditRequest;
    private final Consumer<t2_schedule> onDeleteRequest;

    public DayCell(LocalDate date,
                   YearMonth shownMonth,
                   List<t2_schedule> events,
                   Consumer<LocalDate> onAddRequest,
                   Consumer<t2_schedule> onEditRequest,
                   Consumer<t2_schedule> onDeleteRequest) {

        this.date = date;
        this.shownMonth = shownMonth;
        this.onAddRequest = onAddRequest;
        this.onEditRequest = onEditRequest;
        this.onDeleteRequest = onDeleteRequest;

        setPrefSize(150, 120);
        setPadding(new Insets(4));
        setSpacing(3);
        setStyle("-fx-border-color: #333; -fx-border-width: 1;");

        boolean inMonth = date.getMonth().equals(shownMonth.getMonth());
        boolean isSun = date.getDayOfWeek().getValue() == 7;
        boolean isSat = date.getDayOfWeek().getValue() == 6;

        if (!inMonth) setOpacity(0.35);
        if (isSun) setStyle(getStyle() + "-fx-background-color: #ffe9e9;");
        if (isSat) setStyle(getStyle() + "-fx-background-color: #e9edff;");

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font(14));
        if (isSun) dayLabel.setStyle("-fx-text-fill: #c40000;");
        if (isSat) dayLabel.setStyle("-fx-text-fill: #0029c4;");

        // 일정들을 담을 박스
        VBox eventsBox = new VBox(2);
        eventsBox.setFillWidth(true);

        // 스크롤 가능한 영역
        ScrollPane scroll = new ScrollPane(eventsBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefViewportHeight(80);

        // 이벤트 라벨들 생성
        for (t2_schedule e : events) {
            Label ev = new Label(e.getStartAt().toLocalTime().toString() + " " + e.getTitle());
            ev.setStyle("-fx-background-color: white; -fx-border-color: #888; -fx-padding: 2 3 2 3;");

            // 더블클릭수정
            ev.setOnMouseClicked(me -> {
                if (me.getButton() == MouseButton.PRIMARY && me.getClickCount() == 2) {
                    if (onEditRequest != null) onEditRequest.accept(e);
                }
            });

            // 우클릭 메뉴 수정 / 삭제
            ContextMenu menu = new ContextMenu();
            MenuItem edit = new MenuItem("수정");
            MenuItem delete = new MenuItem("삭제");
            edit.setOnAction(ae -> {
                if (onEditRequest != null) onEditRequest.accept(e);
            });
            delete.setOnAction(ae -> {
                if (onDeleteRequest != null) onDeleteRequest.accept(e);
            });
            menu.getItems().addAll(edit, delete);
            ev.setContextMenu(menu);

            eventsBox.getChildren().add(ev);
        }

        getChildren().addAll(dayLabel, scroll);

        // 날짜 칸 빈 공간 더블클릭/우클릭 새 일정 추가
        setOnMouseClicked(me -> {
            if (me.getButton() == MouseButton.PRIMARY && me.getClickCount() == 2) {
                if (onAddRequest != null) onAddRequest.accept(date);
            }
        });
    }
}