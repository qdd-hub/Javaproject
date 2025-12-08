package com.schedulerfx.model;

import java.time.LocalDateTime;

public class t2_schedule {

    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String category;
    private int remindMinutes;

    
    // EventDialog 에서 쓰는 생성자: (제목, 시작시간, 종료시간, 카테고리, 알림)
    public t2_schedule(String title,
                       LocalDateTime startAt,
                       LocalDateTime endAt,
                       String category,
                       int remindMinutes) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.category = category;
        this.remindMinutes = remindMinutes;
    }

    public t2_schedule(Long id,
                       String title,
                       LocalDateTime startAt,
                       LocalDateTime endAt,
                       String category,
                       int remindMinutes) {
        this(title, startAt, endAt, category, remindMinutes);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getRemindMinutes() {
        return remindMinutes;
    }

    public void setRemindMinutes(int remindMinutes) {
        this.remindMinutes = remindMinutes;
    }

    @Override
    public String toString() {
        return "t2_schedule{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", category='" + category + '\'' +
                ", remindMinutes=" + remindMinutes +
                '}';
    }
}
