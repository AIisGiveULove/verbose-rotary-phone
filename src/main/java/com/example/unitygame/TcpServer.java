package com.example.unitygame;

import com.example.model.NetData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TcpServer {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 房间到Socket列表的映射
    private Map<Integer, List<PlayerSocket>> roomSockets = new ConcurrentHashMap<>();

    // 玩家名到Socket的映射
    private Map<String, PlayerSocket> playerSockets = new ConcurrentHashMap<>();

    public TcpServer(ObjectMapper objectMapper, RedisTemplate<String, Object> redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    // 玩家连接处理
    public void joinRoom(int roomId, String playerName, Socket socket) throws IOException {
        PlayerSocket playerSocket = new PlayerSocket(socket, playerName);

        // 加强同步范围
        synchronized (roomSockets) {
            List<PlayerSocket> socketList = roomSockets.computeIfAbsent(roomId, k -> new ArrayList<>());

            // 防止重复添加
            if (socketList.stream().noneMatch(ps -> ps.getPlayerName().equals(playerName))) {
                socketList.add(playerSocket);
                System.out.println("玩家 " + playerName + " 加入房间 " + roomId);
            }
        }

        synchronized (playerSockets) {
            playerSockets.put(playerName, playerSocket);
        }

        // 打印当前房间状态
        printRoomPlayers(roomId);
    }

    // 发送数据给房主
    public void sendToHost(int roomId, NetData data) throws IOException {
        String hostName = getRoomHost(roomId);
        PlayerSocket hostSocket = playerSockets.get(hostName);
        if (hostSocket != null) {
            sendData(hostSocket, data);
        }
    }

    // 广播数据给房间内其他玩家
    public void broadcastToRoom(int roomId, NetData data, String excludePlayer) throws IOException {
        List<PlayerSocket> sockets = roomSockets.get(roomId);
        if (sockets != null) {
            for (PlayerSocket socket : sockets) {
                if (!socket.getPlayerName().equals(excludePlayer)) {
                    sendData(socket, data);
                }
            }
        }
    }

    // 获取房主名(需实现)
    private String getRoomHost(int roomId) {
        String hostKey="chat_room:"+roomId+":host";
        return (String) redisTemplate.opsForValue().get(hostKey);
    }

    public void printRoomPlayers(int roomId) {
        // 获取房间内的所有玩家Socket
        List<PlayerSocket> sockets = roomSockets.getOrDefault(roomId, Collections.emptyList());

        System.out.println("===== 房间 " + roomId + " 玩家列表 =====");
        System.out.println("总玩家数: " + sockets.size());

        // 遍历打印每个玩家信息
        for (PlayerSocket playerSocket : sockets) {
            Socket socket = playerSocket.getSocket();
            String playerName = playerSocket.getPlayerName();

            System.out.println("玩家名: " + playerName);
            System.out.println("Socket状态: " + (socket.isClosed() ? "已关闭" : "活跃"));
            System.out.println("本地地址: " + socket.getLocalAddress() + ":" + socket.getLocalPort());
            System.out.println("远程地址: " + socket.getInetAddress() + ":" + socket.getPort());
            System.out.println("----------------------------------");
        }
    }

    // 玩家Socket包装类
    private static class PlayerSocket {
        private Socket socket;
        private String playerName;

        public PlayerSocket(Socket socket, String playerName) {
            this.socket = socket;
            this.playerName = playerName;
        }


        public Socket getSocket() {
            return socket;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    // 发送数据到指定Socket
    private void sendData(PlayerSocket socket, NetData data) throws IOException {
        String message = objectMapper.writeValueAsString(data);
        byte[] messageBytes = message.getBytes("UTF-8");
        byte[] lengthBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(messageBytes.length)
                .array();

        OutputStream out = socket.getSocket().getOutputStream();
        out.write(lengthBytes);
        out.write(messageBytes);
        out.flush();
    }

    @PostConstruct
    public void start() throws IOException {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(10001)) {
                serverSocket.setSoTimeout(1000); // 设置accept超时
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        // 为每个客户端连接创建新线程
                        new Thread(() -> {
                            try {
                                handleClient(clientSocket);
                            } catch (IOException e) {
                                System.err.println("客户端处理异常: " + e.getMessage());
                            }
                        }).start();
                    } catch (SocketTimeoutException e) {
                        // 超时继续循环，避免永久阻塞
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) throws IOException {
        // 移出try-with-resources，改为手动管理
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            clientSocket.setSoTimeout(5000);
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            while (!clientSocket.isClosed()) {  // 显式检查连接状态
                try {
                    // 1. 读取消息长度
                    byte[] lengthBytes = new byte[4];
                    in.readFully(lengthBytes);

                    int messageLength = ByteBuffer.wrap(lengthBytes)
                            .order(ByteOrder.BIG_ENDIAN)
                            .getInt();

                    // 2. 读取消息体
                    byte[] messageBytes = new byte[messageLength];
                    in.readFully(messageBytes);
                    String message = new String(messageBytes, "UTF-8");

                    NetData data = objectMapper.readValue(message, NetData.class);
                    System.out.println("收到数据: " + data.netAccount.getId() + ":"+data.netAccount.getName());

                    // 处理消息
                    if ("JOIN".equals(data.getType())) {
                        joinRoom(data.netAccount.getId(), data.netAccount.getName(), clientSocket);
                    }
                    if("BroadCast".equals(data.getType())) {
                        broadcastToRoom(data.netAccount.getId(),data,"null");
                    }
                    if("Host".equals(data.getType())) {
                        sendToHost(data.netAccount.getId(),data);
                    }

                    // 3. 发送响应（可选心跳机制）
//                    String response = "ACK"; // 改为简单确认响应
//                    byte[] responseBytes = response.getBytes("UTF-8");
//                    out.writeInt(responseBytes.length);
//                    out.write(responseBytes);
//                    out.flush();

                } catch (SocketTimeoutException e) {
                    // 添加读取超时处理（需先设置setSoTimeout）
                    //System.out.println("读取超时，继续等待...");
                    continue;
                } catch (EOFException e) {
                    System.out.println("客户端优雅断开");
                    break;
                } catch (SocketException e) {
                    System.out.println("连接异常: " + e.getMessage());
                    break;
                }
            }
        } finally {
            // 手动关闭资源
            try { if (in != null) in.close(); } catch (IOException e) { /* 忽略 */ }
            try { if (out != null) out.close(); } catch (IOException e) { /* 忽略 */ }
            try { clientSocket.close(); } catch (IOException e) { /* 忽略 */ }
            System.out.println("连接已关闭");
        }
    }
}
