package vn.xuandat.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;
import vn.xuandat.backend.dto.response.CategoryResponse;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

//    Bật xử lý annotation cache.
//    Lưu CategoryResponse dưới dạng JSON.
//    Đặt TTL 60 giây và không cache null.
//    Chỉ khai báo cache category; các cache chứa kiểu dữ liệu khác cần cấu hình riêng.
//    Dùng transactionAware() để thao tác ghi/xóa cache phối hợp với transaction và thực hiện sau commit thành công.

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            JsonMapper jsonMapper
    ) {
        var serializer = new JacksonJsonRedisSerializer<>(
                jsonMapper,
                CategoryResponse.class
        );

        var categoryConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(categoryConfig)
                .withInitialCacheConfigurations(
                        Map.of("category", categoryConfig)
                )
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}