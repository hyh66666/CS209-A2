package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatServer {
    private static final int PORT = 1234;
    private static final List<ClientThread> clients = new ArrayList<>();

    private static final HashMap<String, List<ClientThread>> groupMap = new HashMap<>();

    public void Run() throws IOException {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);
                ClientThread client = new ClientThread(socket, this);
                clients.add(client);
                Thread t = new Thread(client);
                t.start();
            }
    }

    public synchronized boolean userValid(String userName) {
        boolean flag = true;
        int count = 0;
        for (ClientThread client : clients) {
            if (client.getUsername().equals(userName)) {
                count++;
                if (count == 2){
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }
    public synchronized void broadcast(String message, ClientThread sender) {
        for (ClientThread client : clients) {
            if (client != sender) {
                client.sendMessage("[Broadcast] from " + sender.getUsername() + ": " + message);
            }
        }
    }


    public synchronized void sendPrivateMessage(String message, ClientThread sender, String recipient) throws IOException {
        for (ClientThread client : clients) {
            if (client.getUsername().equals(recipient)) {
                client.sendMessage("From " + sender.getUsername() + ": " + message);
                return;
            }
        }
        sender.sendMessage("User " + recipient + " not found");
    }

    public synchronized void creatGroup(String name, String members) {
        String[] strings = members.split(",");
        List<ClientThread> tempList = new ArrayList<>();
        for (String string : strings) {
            for (ClientThread client : clients) {
                if (client.getUsername().equals(string)) {
                    tempList.add(client);
                    client.getGroup().add(name);
                }
            }
        }
        groupMap.put(name, tempList);
        System.out.println("group created!");
    }

    public synchronized void sendGroupMessage(String message, String name, ClientThread sender) throws IOException {
        for (ClientThread client : groupMap.get(name)) {
            if (client != sender) {
                client.sendMessage("From [" + name + "] " + sender.getUsername() + ": " + message);
            }
        }
    }

    public synchronized void listGroup(ClientThread client) {
        client.getOut().println(Arrays.toString(client.getGroup().toArray()));
        client.getOut().flush();
    }

    public synchronized void removeClient(ClientThread clientThread) {
        clients.remove(clientThread);
        System.out.println("Client disconnected: " + clientThread.getSocket());
    }
}
