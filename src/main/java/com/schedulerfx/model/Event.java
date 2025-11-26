package com.schedulerfx.model;

import java.time.LocalDateTime;

public class Event {
	private Long id;
	private String title;
	private LocalDateTime startAt;
	private LocalDateTime endAt; // 필요 없으면 null 허용
	private int remindMinutes; // 60,30,15,5 등

	public Event(Long id, String title, LocalDateTime startAt, LocalDateTime endAt, int remindMinutes) {
		this.id = id;
		this.title = title;
		this.startAt = startAt;
		this.endAt = endAt;
		this.remindMinutes = remindMinutes;
	}

	public Event(String title, LocalDateTime startAt, LocalDateTime endAt, int remindMinutes) {
		this(null, title, startAt, endAt, remindMinutes);
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public LocalDateTime getStartAt() {
		return startAt;
	}

	public LocalDateTime getEndAt() {
		return endAt;
	}

	public int getRemindMinutes() {
		return remindMinutes;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
