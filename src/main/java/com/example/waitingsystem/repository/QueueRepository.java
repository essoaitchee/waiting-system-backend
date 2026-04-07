package com.example.waitingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.example.waitingsystem.domain.QueueEntry;
import com.example.waitingsystem.domain.QueueEvent;
import org.apache.ibatis.annotations.Param;

public interface QueueRepository {

	QueueEvent findActiveEventById(@Param("eventId") Long eventId);

	QueueEvent findEventById(@Param("eventId") Long eventId);

	List<QueueEvent> findActiveEvents();

	int upsertDemoEvent(QueueEvent queueEvent);

	QueueEntry findEntryByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") String userId);

	QueueEntry findEntryByAdmissionToken(@Param("eventId") Long eventId, @Param("admissionToken") String admissionToken);

	void insertQueueEntry(QueueEntry queueEntry);

	int updateEntryAsAdmitted(
		@Param("eventId") Long eventId,
		@Param("userId") String userId,
		@Param("admissionToken") String admissionToken,
		@Param("admittedAt") LocalDateTime admittedAt,
		@Param("admissionExpiresAt") LocalDateTime admissionExpiresAt,
		@Param("updatedAt") LocalDateTime updatedAt
	);

	int updateEntryAsEntered(
		@Param("eventId") Long eventId,
		@Param("admissionToken") String admissionToken,
		@Param("updatedAt") LocalDateTime updatedAt
	);

	int updateEntryAsExpired(
		@Param("eventId") Long eventId,
		@Param("userId") String userId,
		@Param("updatedAt") LocalDateTime updatedAt
	);

	int updateEntryForRejoin(
		@Param("eventId") Long eventId,
		@Param("userId") String userId,
		@Param("queueToken") String queueToken,
		@Param("queueSequence") Long queueSequence,
		@Param("enteredAt") LocalDateTime enteredAt,
		@Param("updatedAt") LocalDateTime updatedAt
	);

	int resetEntryForDemo(
		@Param("eventId") Long eventId,
		@Param("userId") String userId,
		@Param("updatedAt") LocalDateTime updatedAt
	);

	int resetEntriesForDemoByUserPrefix(
		@Param("eventId") Long eventId,
		@Param("userIdPrefix") String userIdPrefix,
		@Param("updatedAt") LocalDateTime updatedAt
	);
}
