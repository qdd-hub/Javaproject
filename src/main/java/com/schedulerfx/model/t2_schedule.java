package com.schedulerfx.model;

import java.time.LocalDateTime;

public class t2_schedule {
	private Long id;
	private String title;
	private LocalDateTime startAt;
	private LocalDateTime endAt; // 필요 없으면 null 허용
	private String category;
	private int remindMinutes; // 60,30,15,5 등
	

	public t2_schedule(Long id, String title, LocalDateTime startAt, LocalDateTime endAt, String category,int remindMinutes) {
		this.id = id;
		this.title = title;
		this.startAt = startAt;
		this.endAt = endAt;
		this.category = category;
		this.remindMinutes = remindMinutes;
	}

	public t2_schedule(String title, LocalDateTime startAt, LocalDateTime endAt, String category, int remindMinutes) {
		this(null, title, startAt, endAt, category, remindMinutes);
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

	public String getCategory() {
		return category;
	}
	
	// 이 항목들에도 수정기능 넣을건지 확인
//	public void setTitle(String title) {
//		this.title = title;
//	}
//
//	public void setStartAt(LocalDateTime startAt) {
//		this.startAt = startAt;
//	}
//
//	public void setEndAt(LocalDateTime endAt) {
//		this.endAt = endAt;
//	}
//
//	public void setCategory(String category) {
//		this.category = category;
//	}
//
//	public void setRemindMinutes(int remindMinutes) {
//		this.remindMinutes = remindMinutes;
//	}
	
}


