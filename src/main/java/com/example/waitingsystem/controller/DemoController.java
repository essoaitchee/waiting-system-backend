package com.example.waitingsystem.controller;

import com.example.waitingsystem.dto.DemoJoinRequest;
import com.example.waitingsystem.dto.DemoMonitorResponse;
import com.example.waitingsystem.dto.DemoRoundResponse;
import com.example.waitingsystem.service.DemoExperienceService;
import com.example.waitingsystem.service.DemoRoundService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

	private final DemoExperienceService demoExperienceService;
	private final DemoRoundService demoRoundService;

	public DemoController(DemoExperienceService demoExperienceService, DemoRoundService demoRoundService) {
		this.demoExperienceService = demoExperienceService;
		this.demoRoundService = demoRoundService;
	}

	@PostMapping("/join")
	public DemoMonitorResponse joinExperience(@RequestBody DemoJoinRequest request) {
		return demoExperienceService.joinExperience(request);
	}

	@GetMapping("/monitor")
	public DemoMonitorResponse getMonitor(
		@RequestParam(required = false) Long eventId,
		@RequestParam(required = false) Long couponId,
		@RequestParam @NotBlank String userId
	) {
		return demoExperienceService.getMonitor(eventId, couponId, userId);
	}

	@GetMapping("/round")
	public DemoRoundResponse getRound(@RequestParam @NotBlank String userId) {
		return demoRoundService.getRound(userId);
	}
}
