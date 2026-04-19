package com.fourarcade.arcadebackend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    // Redis 와 연결을 위한 Factory
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        // Redis 기본 접속 정보 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);

        // Lettuce 클라이언트 옵션 설정
        // Lettuce - 스프링부트가 기본으로 사용하는 고성능 비동기 Redis 클라이언트 라이브러리
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl() // TLS 설정
                .build();

        // 커넥션팩토리 생성 및 변환
        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // 직렬화 설정
        // Key: 일반적인 문자열
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // ObjectMapper 세팅
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        RedisSerializer<Object> customJsonSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object t) throws SerializationException {
                if (t == null) return new byte[0];
                try {
                    return objectMapper.writeValueAsBytes(t);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("JSON 직렬화 에러", e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return objectMapper.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new SerializationException("JSON 역직렬화 에러", e);
                }
            }
        };

        // Value: custom serializer
        redisTemplate.setValueSerializer(customJsonSerializer);
        redisTemplate.setHashValueSerializer(customJsonSerializer);

        return redisTemplate;
    }
}
