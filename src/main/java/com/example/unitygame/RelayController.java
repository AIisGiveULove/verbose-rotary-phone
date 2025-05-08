package com.example.unitygame;

import com.example.model.NetChatData;
import com.example.model.NetData;
import com.example.model.PCControlData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class RelayController {

    @Autowired
    private TcpServer tcpServer;
    private ObjectMapper objectMapper;

    // 存储房间与房主映射
    private Map<Integer, String> roomHosts = new ConcurrentHashMap<>();

    // 房主开始游戏
    @PostMapping("/startGame")
    public String startGame(@RequestBody NetChatData request) {
        roomHosts.put(request.getRoomId(), request.getName());
        tcpServer.printRoomPlayers(request.getRoomId());
        NetData netData = new NetData();
        netData.timestamp = System.currentTimeMillis();
        netData.Type="StartGame";
        netData.playerData=new PCControlData();
        netData.playerData.PCinFollow= (int) (Math.random()*1000000+1);
        try {
            tcpServer.broadcastToRoom(request.getRoomId(),netData,"null");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Game started. You are the host.";
    }

    // 转发游戏数据
    @PostMapping("/relay")
    public void relayGameData(@RequestBody RelayRequest request) throws IOException {
        if (request.getTarget().equals("host")) {
            // 转发给房主
            tcpServer.sendToHost(request.getRoomId(), request.getData());
        } else {
            // 广播给其他玩家
            tcpServer.broadcastToRoom(request.getRoomId(), request.getData(), request.getExcludePlayer());
        }
    }

    // 房主迁移
    @PostMapping("/migrateHost")
    public String migrateHost(@RequestBody NetChatData request) {
        roomHosts.put(request.getRoomId(), request.getName());
        return "New host: " + request.getName();
    }

    public static class RelayRequest {
        private int roomId;
        private String target; // "host" or "others"
        private String excludePlayer;
        private NetData data;

        public int getRoomId() {
            return roomId;
        }

        public String getTarget() {
            return target;
        }

        public String getExcludePlayer() {
            return excludePlayer;
        }

        public NetData getData() {
            return data;
        }

        // getters & setters
    }
}