package com.newstoss.global.kis.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newstoss.global.kis.dto.KisApiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KisApiStreamProducer {

    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long DEDUP_TTL = 30;

    /**
     * 주식 현재가를 저장하는 메서드
     * @param stockCode 주식 코드
     */
    public void sendStockRequest(String stockCode) {
        String dedupKey = "stream-dedup:stock:" + stockCode;

        // setIfAbsent는 key가 없으면 true, 있으면 false 반환
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL, java.util.concurrent.TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(isFirst)) {
            KisApiRequestDto dto = new KisApiRequestDto("stock", stockCode, null, null);
            Map<String, Object> map = new ObjectMapper().convertValue(dto, new TypeReference<>() {});
            redisTemplate.opsForStream().add("kis-api-request", map);
        }
    }

    /**
     * 환율 Fx 정보를 Redis Stream에 producing 하는 메서드
     * @param FxType Fx 맵에서 타입값
     * @param FxCode Fx 맵에서 코드값
     */
    public void sendFxRequest(String FxType, String FxCode) {
        KisApiRequestDto dto = new KisApiRequestDto("fx", null, FxType, FxCode);
        Map<String, Object> map = new ObjectMapper().convertValue(dto, new TypeReference<>() {});
        redisTemplate.opsForStream().add("kis-api-request", map);
    }
}
