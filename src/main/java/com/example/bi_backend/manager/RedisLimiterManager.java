package com.example.bi_backend.manager;

import com.example.bi_backend.common.ErrorCode;
import com.example.bi_backend.exception.BusinessException;
import jakarta.annotation.Resource;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 用户请求频繁限制器
     */
    public void doRateLimit(String key) {
        RRateLimiter limiter = redissonClient.getRateLimiter(key);
        //设定限制规则
        limiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);

        boolean canDo = limiter.tryAcquire(1);
        if (!canDo) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
