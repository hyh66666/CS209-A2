package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        ChatServer chatServer = new ChatServer();
        chatServer.Run();
    }
}
