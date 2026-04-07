package com.example.waitingsystem.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.waitingsystem.common.exception.ConflictException;
import com.example.waitingsystem.dto.AuthLoginRequest;
import com.example.waitingsystem.dto.AuthLoginResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final StringRedisTemplate stringRedisTemplate;
	private final Clock clock;

	public AuthService(StringRedisTemplate stringRedisTemplate, Clock clock) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.clock = clock;
	}

	public AuthLoginResponse loginOrRegister(AuthLoginRequest request) {
		String loginId = request.loginId().trim();
		String password = request.password().trim();
		String userKey = userKey(loginId);
		Map<Object, Object> existingUser = stringRedisTemplate.opsForHash().entries(userKey);
		String passwordHash = sha256(password);
		String now = LocalDateTime.now(clock).format(DATE_TIME_FORMATTER);

		if (existingUser == null || existingUser.isEmpty()) {
			Map<String, String> values = new LinkedHashMap<>();
			values.put("loginId", loginId);
			values.put("passwordHash", passwordHash);
			values.put("createdAt", now);
			values.put("lastLoginAt", now);
			stringRedisTemplate.opsForHash().putAll(userKey, values);
			return new AuthLoginResponse(loginId, true, "신규 사용자로 바로 가입하고 로그인했습니다.");
		}

		String storedPasswordHash = String.valueOf(existingUser.getOrDefault("passwordHash", ""));
		if (!passwordHash.equals(storedPasswordHash)) {
			throw new ConflictException("비밀번호가 일치하지 않습니다.");
		}

		stringRedisTemplate.opsForHash().put(userKey, "lastLoginAt", now);
		return new AuthLoginResponse(loginId, false, "기존 사용자 정보로 로그인했습니다.");
	}

	private String userKey(String loginId) {
		return "auth:user:" + loginId;
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (byte current : hashed) {
				builder.append(String.format("%02x", current));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
		}
	}
}
