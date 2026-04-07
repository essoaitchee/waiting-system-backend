package com.example.waitingsystem.controller;

import com.example.waitingsystem.dto.QueueAdmissionConsumeRequest;
import com.example.waitingsystem.dto.QueueAdmissionConsumeResponse;
import com.example.waitingsystem.dto.QueueAdmissionRequest;
import com.example.waitingsystem.dto.QueueAdmissionResponse;
import com.example.waitingsystem.dto.QueueEnterRequest;
import com.example.waitingsystem.dto.QueueEnterResponse;
import com.example.waitingsystem.dto.QueueStatusResponse;
import com.example.waitingsystem.service.QueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

	private final QueueService queueService;

	public QueueController(QueueService queueService) {
		this.queueService = queueService;
	}

	@PostMapping("/enter")
	public QueueEnterResponse enterQueue(@Valid @RequestBody QueueEnterRequest request) {
		return queueService.enterQueue(request);
	}

	@GetMapping("/status")
	public QueueStatusResponse getStatus(
		@RequestParam @NotNull(message = "eventId는 필수입니다.") Long eventId,
		@RequestParam @NotBlank(message = "userId는 필수입니다.") String userId
	) {
		return queueService.getStatus(eventId, userId);
	}

	@PostMapping("/admit")
	public QueueAdmissionResponse admit(@Valid @RequestBody QueueAdmissionRequest request) {
		return queueService.admitNextBatch(request);
	}

	@PostMapping("/admission/consume")
	public QueueAdmissionConsumeResponse consumeAdmission(@Valid @RequestBody QueueAdmissionConsumeRequest request) {
		return queueService.consumeAdmission(request);
	}
}
