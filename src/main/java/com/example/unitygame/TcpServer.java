package com.example.unitygame;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Component
public class TcpServer {

    @PostConstruct
    public void start() throws IOException {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(10001)) {
                System.out.println("Binary TCP Server started on port 10001");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) throws IOException {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            // 1. 读取消息长度 (4字节大端序)
            byte[] lengthBytes = new byte[4];
            in.readFully(lengthBytes);
            int messageLength = ByteBuffer.wrap(lengthBytes)
                    .order(ByteOrder.BIG_ENDIAN)
                    .getInt();

            // 2. 读取消息体
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);
            String message = new String(messageBytes, "UTF-8");
            System.out.println("Received from client: " + message);

            // 3. 准备响应
            String response = "Hello from Binary TCP Server!";
            byte[] responseBytes = response.getBytes("UTF-8");

            // 4. 发送响应 (长度前缀 + 消息体)
            out.writeInt(responseBytes.length); // 自动使用大端序
            out.write(responseBytes);
            out.flush();

        } finally {
            clientSocket.close();
        }
    }
}
