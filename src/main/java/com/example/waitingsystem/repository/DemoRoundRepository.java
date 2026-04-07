package com.example.waitingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.example.waitingsystem.domain.DemoClickRecord;
import org.apache.ibatis.annotations.Param;

public interface DemoRoundRepository {

	DemoClickRecord findRoundClickByUserId(
		@Param("userId") String userId,
		@Param("roundStartedAt") LocalDateTime roundStartedAt
	);

	void insertClickRecord(DemoClickRecord clickRecord);

	DemoClickRecord findLatestClickRecordByUserId(@Param("userId") String userId);

	DemoClickRecord findBestClickRecordByUserId(@Param("userId") String userId);

	List<DemoClickRecord> findTopClickRecords(@Param("limit") int limit);
}
