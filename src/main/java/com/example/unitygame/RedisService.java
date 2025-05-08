package com.example.unitygame;

import com.example.model.NetData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 存储NetData到Redis
     * @param key 键
     * @param netData 值
     * @param timeout 过期时间(秒)
     */
    public void setNetData(String key, NetData netData, long timeout) {
        redisTemplate.opsForValue().set(key, netData, timeout, TimeUnit.SECONDS);
    }

    /**
     * 从Redis获取NetData
     * @param key 键
     * @return NetData对象
     */
    public NetData getNetData(String key) {
        return (NetData) redisTemplate.opsForValue().get(key);
    }

    /**
     * 存储NetData到Redis Hash
     * @param hashKey Hash键
     * @param key 键
     * @param netData 值
     */
    public void setNetDataToHash(String hashKey, String key, NetData netData) {
        redisTemplate.opsForHash().put(hashKey, key, netData);
    }

    /**
     * 从Redis Hash获取NetData
     * @param hashKey Hash键
     * @param key 键
     * @return NetData对象
     */
    public NetData getNetDataFromHash(String hashKey, String key) {
        return (NetData) redisTemplate.opsForHash().get(hashKey, key);
    }
}