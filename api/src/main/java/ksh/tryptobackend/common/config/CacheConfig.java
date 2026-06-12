package ksh.tryptobackend.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String WALLET_OWNER_CACHE = "walletOwner";

    private static final long WALLET_OWNER_MAX_SIZE = 100_000L;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(WALLET_OWNER_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(WALLET_OWNER_MAX_SIZE));
        return cacheManager;
    }
}
