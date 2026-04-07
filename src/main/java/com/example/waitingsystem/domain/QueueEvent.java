package com.example.waitingsystem.domain;

import java.time.LocalDateTime;

public class QueueEvent {

	private Long eventId;
	private String eventName;
	private Integer capacityPerSecond;
	private Integer admissionWindowSeconds;
	private String eventStatus;
	private String activeYn;
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public Integer getCapacityPerSecond() {
		return capacityPerSecond;
	}

	public void setCapacityPerSecond(Integer capacityPerSecond) {
		this.capacityPerSecond = capacityPerSecond;
	}

	public Integer getAdmissionWindowSeconds() {
		return admissionWindowSeconds;
	}

	public void setAdmissionWindowSeconds(Integer admissionWindowSeconds) {
		this.admissionWindowSeconds = admissionWindowSeconds;
	}

	public String getEventStatus() {
		return eventStatus;
	}

	public void setEventStatus(String eventStatus) {
		this.eventStatus = eventStatus;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}

	public LocalDateTime getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(LocalDateTime startsAt) {
		this.startsAt = startsAt;
	}

	public LocalDateTime getEndsAt() {
		return endsAt;
	}

	public void setEndsAt(LocalDateTime endsAt) {
		this.endsAt = endsAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
