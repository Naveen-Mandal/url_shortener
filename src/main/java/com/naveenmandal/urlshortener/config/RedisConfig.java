package com.naveenmandal.urlshortener.config;

import com.naveenmandal.urlshortener.listener.ClickListener;
import com.naveenmandal.urlshortener.service.RedisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration  // tells Spring: "this class has Bean definitions"
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for both key and value
        // Without this, Redis stores ugly binary data like \xac\xed\x00\x05
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChannelTopic clickTopic() {
        return new ChannelTopic(RedisService.CLICKS_TOPIC);
    }

    /**
     * Background message listener container.
     * Manages worker threads that listen to incoming Redis messages.
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            ClickListener clickListener,
            ChannelTopic clickTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(clickListener, clickTopic);
        return container;
    }
}