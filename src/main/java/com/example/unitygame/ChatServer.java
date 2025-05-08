package com.example.unitygame;

import com.example.model.NetChatData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
public class ChatServer {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private static final String ROOM_PREFIX = "chat_room:";
    private static final String USER_PREFIX = "chat_user:";

    public ChatServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/selectChatRoom")
    public int buildRoom(@RequestBody NetChatData data) {
        if(data.getRoomId() == 0) {
            // 创建新房间
            int roomId = generateRoomId();
            String roomKey = ROOM_PREFIX + roomId;

            // 存储房间信息到Redis，设置过期时间(例如24小时)
            redisTemplate.opsForValue().set(roomKey, "active", 24, TimeUnit.HOURS);

            // 创建用户集合
            String usersKey = roomKey + ":users";
            redisTemplate.opsForSet().add(usersKey, data.getName());

            //设置房主
            String hostKey = roomKey + ":host";
            redisTemplate.opsForSet().add(hostKey, data.getName());

            return roomId;
        } else {
            // 加入现有房间
            String roomKey = ROOM_PREFIX + data.getRoomId();
            if(redisTemplate.hasKey(roomKey)) {
                String usersKey = roomKey + ":users";
                redisTemplate.opsForSet().add(usersKey, data.getName());
                return -1; // 表示加入成功
            }
            return 0; // 房间不存在
        }
    }

    @GetMapping("/checkRoom")
    public List<Integer> getRoomList() {
        // 使用Redis keys命令查找所有聊天房间
        Set<String> roomKeys = redisTemplate.keys(ROOM_PREFIX + "*");

        List<Integer> roomList = new ArrayList<>();

        if (roomKeys != null) {
            for (String key : roomKeys) {
                // 提取房间ID (去掉前缀)
                String roomIdStr = key.substring(ROOM_PREFIX.length());

                // 检查是否是主房间键 (避免包含子键如 ":users")
                if (!roomIdStr.contains(":")) {
                    try {
                        int roomId = Integer.parseInt(roomIdStr);

                        // 检查房间是否活跃
                        if (redisTemplate.hasKey(key)) {
                            roomList.add(roomId);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式错误的键
                    }
                }
            }
        }
        return roomList;
    }

    @GetMapping("/roomInfo/{roomId}")
    public Map<String, Object> getRoomInfo(@PathVariable int roomId) {
        String roomKey = ROOM_PREFIX + roomId;
        Map<String, Object> result = new HashMap<>();

        if (!redisTemplate.hasKey(roomKey)) {
            result.put("error", "Room not found");
            return result;
        }

        // 获取房间用户数
        String usersKey = roomKey + ":users";
        Long userCount = redisTemplate.opsForSet().size(usersKey);

        // 获取房间创建时间
        Long createTime = redisTemplate.getExpire(roomKey, TimeUnit.SECONDS);

        result.put("roomId", roomId);
        result.put("userCount", userCount != null ? userCount : 0);
        result.put("timeLeft", createTime != null ? createTime : 0);

        return result;
    }

    @GetMapping("/getOthers")
    public List<String> getOthers(@RequestParam String roomId) {
        //返回用户列表
        String roomKey = ROOM_PREFIX + roomId + ":users";
        return new ArrayList<>(redisTemplate.keys(roomKey));
    }

    @PostMapping("/chat")  // Changed to PostMapping as we're sending data
    public ResponseEntity<String> chat(@RequestBody NetChatData data) {
        try {
            String roomKey = ROOM_PREFIX + data.getRoomId();

            // Check if room exists
            if(!redisTemplate.hasKey(roomKey)) {
                return ResponseEntity.badRequest().body("Room does not exist");
            }

            // Get all users in the room
            String usersKey = roomKey + ":users";
            Set<String> users = redisTemplate.opsForSet().members(usersKey);

            if(users != null && !users.isEmpty()) {
                // Convert NetChatData to JSON string
                String message = objectMapper.writeValueAsString(data);

                // Broadcast message to all users in the room
                for(String user : users) {
                    String userQueueKey = USER_PREFIX + user + ":messages";
                    redisTemplate.opsForList().rightPush(userQueueKey, message);

                    // Optional: Set expiration for user message queue (e.g., 1 day)
                    redisTemplate.expire(userQueueKey, 1, TimeUnit.DAYS);
                }

                return ResponseEntity.ok("Message sent successfully");
            }

             return ResponseEntity.badRequest().body("No users in the room");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing chat message: " + e.getMessage());
        }
    }

    private int generateRoomId() {
        return 10000 + random.nextInt(90000); // 生成5位房间号
    }
}