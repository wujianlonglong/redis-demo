package com.wjl.redisdemo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import java.util.Collections;

@Component
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String LOCK_SUCCESS = "OK";

    private final String SET_IF_NOT_EXIST = "NX";

    private final String SET_WITH_EXPIRE_TIME = "PX";

    private final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 100;


    /**
     * 尝试获取锁
     *
     * @param lockKey    锁的key
     * @param lockValue  锁持有者的唯一标识（推荐使用uuid，防止解锁时误删）
     * @param expireTime 锁的存活时间 （防止死锁，暂且不用守护线程刷新存活时间）
     * @param waitTime   尝试获取锁的等待时间
     * @return
     */
    public boolean lock(String lockKey, String lockValue, long expireTime, long waitTime) throws InterruptedException {
        while (waitTime >= 0) {
            if (setNxWithExpire(lockKey, lockValue, expireTime)) {
                return true;
            }
            waitTime -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
            Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
        }
        return false;
    }


    /**
     * 解锁--删除指定的缓存
     *
     * @param lockKey
     * @param lockValue
     */
    public void unlock(String lockKey, String lockValue) {
        deleteByLua(lockKey, lockValue);
    }


    private boolean setNxWithExpire(String lockKey, String lockValue, long expireTime) {
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    return connection.execute("set", new byte[][]{
                            SafeEncoder.encode(lockKey), SafeEncoder.encode(lockValue), SafeEncoder.encode(SET_IF_NOT_EXIST),
                            SafeEncoder.encode(SET_WITH_EXPIRE_TIME), Protocol.toByteArray(expireTime)});
                }
            });
        } catch (Exception ex) {
           // log.error("setNxWithExpire redis error,key:{}", lockKey);
        }

        if (obj != null && obj.toString().equals(LOCK_SUCCESS))
            return true;

        return false;
    }

    private void deleteByLua(String lockKey, String lockValue) {
        String LUA_SCRIPT_UNLOCK = "if (redis.call('GET', KEYS[1]) == ARGV[1]) then "
                + "return redis.call('DEL',KEYS[1]) "
                + "else " + "return 0 " + "end";
        RedisScript<Long> scriptUnlock =
                new DefaultRedisScript<Long>(LUA_SCRIPT_UNLOCK,
                        Long.class);

        redisTemplate.execute(scriptUnlock,
                Collections.singletonList(lockKey),
                lockValue);

    }


}
