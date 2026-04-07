package com.example.waitingsystem.config;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
		ObjectMapper cacheObjectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
		cacheObjectMapper.activateDefaultTyping(
			BasicPolymorphicTypeValidator.builder()
				.allowIfSubType(Object.class)
				.build(),
			ObjectMapper.DefaultTyping.EVERYTHING);
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.entryTtl(Duration.ofMinutes(5))
			.serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(SerializationPair.fromSerializer(serializer));

		return RedisCacheManager.builder(
				RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory, BatchStrategies.scan(1000)))
			.cacheDefaults(defaultConfig)
			.withInitialCacheConfigurations(Map.of(
				"product-list-v2", defaultConfig.entryTtl(Duration.ofMinutes(3)),
				"product-detail", defaultConfig.entryTtl(Duration.ofMinutes(10))))
			.enableStatistics()
			.build();
	}
}
