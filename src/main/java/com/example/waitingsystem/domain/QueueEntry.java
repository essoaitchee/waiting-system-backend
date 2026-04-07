package com.example.waitingsystem.domain;

import java.time.LocalDateTime;

public class QueueEntry {

	private Long queueEntryId;
	private Long eventId;
	private String userId;
	private String queueToken;
	private Long queueSequence;
	private String status;
	private LocalDateTime enteredAt;
	private String admissionToken;
	private LocalDateTime admittedAt;
	private LocalDateTime admissionExpiresAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Long getQueueEntryId() {
		return queueEntryId;
	}

	public void setQueueEntryId(Long queueEntryId) {
		this.queueEntryId = queueEntryId;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getQueueToken() {
		return queueToken;
	}

	public void setQueueToken(String queueToken) {
		this.queueToken = queueToken;
	}

	public Long getQueueSequence() {
		return queueSequence;
	}

	public void setQueueSequence(Long queueSequence) {
		this.queueSequence = queueSequence;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getEnteredAt() {
		return enteredAt;
	}

	public void setEnteredAt(LocalDateTime enteredAt) {
		this.enteredAt = enteredAt;
	}

	public String getAdmissionToken() {
		return admissionToken;
	}

	public void setAdmissionToken(String admissionToken) {
		this.admissionToken = admissionToken;
	}

	public LocalDateTime getAdmittedAt() {
		return admittedAt;
	}

	public void setAdmittedAt(LocalDateTime admittedAt) {
		this.admittedAt = admittedAt;
	}

	public LocalDateTime getAdmissionExpiresAt() {
		return admissionExpiresAt;
	}

	public void setAdmissionExpiresAt(LocalDateTime admissionExpiresAt) {
		this.admissionExpiresAt = admissionExpiresAt;
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
