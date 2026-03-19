package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.application.port.out.MarketMetaQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.MarketMetaEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketMetaQueryAdapter implements MarketMetaQueryPort {

    private static final String KEY_PREFIX = "market-meta:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, List<MarketMetaEntry>> findAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            log.warn("Redis에 market-meta 키가 없습니다");
            return Map.of();
        }

        Map<String, List<MarketMetaEntry>> result = new HashMap<>();
        for (String key : keys) {
            String exchangeName = key.substring(KEY_PREFIX.length());
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                continue;
            }

            try {
                List<MarketMetaEntry> entries = objectMapper.readValue(
                        json, new TypeReference<List<MarketMetaEntry>>() {});
                result.put(exchangeName, entries);
                log.info("market-meta:{} 로드 완료: {}건", exchangeName, entries.size());
            } catch (Exception e) {
                log.error("market-meta:{} 파싱 실패", exchangeName, e);
            }
        }
        return result;
    }
}
