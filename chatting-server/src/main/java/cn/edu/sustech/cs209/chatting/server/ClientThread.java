package cn.edu.sustech.cs209.chatting.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientThread implements Runnable {
    private final Socket socket;

    private final ChatServer server;
    private String username = null;
    private BufferedReader in;
    private PrintWriter out;

    public PrintWriter getOut() {
        return out;
    }

    private List<String> groupList = new ArrayList<>();
    public String getUsername() {
        return username;
    }

    public List<String> getGroup() {
        return groupList;
    }
    public Socket getSocket() {
        return socket;
    }
    public void sendMessage(String message) {
        out.println(message);
    }

    public ClientThread(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String line, command = null, recipient = null;

            while (username == null) {
//                out.println("Enter your name:");
                username = in.readLine();
                if (!server.userValid(username)) {
                    username = null;
                    out.println("Name already exists. Please enter another name.");
                } else {
                    out.println("Welcome, " + username + "!");
                }
            }
            int i = 0;
            StringBuilder sb = new StringBuilder();

            label:
            while ((line = in.readLine()) != null) {
                if (i == 0) {
                    String[] strings = line.split(" ");
                    command = strings[0];
                    recipient = strings[1];
                } else {
                    // private
                    switch (command) {
                        // getUsers
                        case "getUsers":
                            server.getUsers(this);
                            break;
                        // broadcast
                        case "broadcast":
                            server.broadcast(line, this);
                            break;
                        // private
                        case "private":
                            server.sendPrivateMessage(line, this, recipient);
                            break;
                        // create group
                        case "create":
                            // recipient is group_name
                            String member = line+","+username;
                            server.creatGroup(recipient, member, this);
                            break;
                        // join group
                        case "join":
                            // recipient is group_name
                            if (server.joinGroup(recipient, this)) {
                                out.println(username+" join the group " +recipient + "!");
                            } else {
                                out.println("Group " +recipient + " is not exist!");
                            }
                            break;
                        // exit group
                        case "exit":
                            // recipient is group_name
                            if (server.exitGroup(recipient, this)) {
                                out.println("Exit the group " +recipient + "!");
                            } else {
                                out.println("You're not in the group "+ recipient);
                            }
                            break;
                        // groupChat
                        case "groupChat":
                            // recipient is group_name
                            if (!groupList.contains(recipient)){
                                out.println("Group " + recipient + " not found");
                                break;
                            }
                            server.sendGroupMessage(line, recipient, this);
                            break;
                        // list groups
                        case "ls":
//                            server.listGroup(this);
                            out.println("Groups: " + Arrays.toString(groupList.toArray()));
                            break;
                        // quit
                        case "quit":
                            server.broadcast("I'm offline", this);
                            server.removeClient(this);
                            out.println("process over!");
                            socket.close();
                            break label;
                        default:
                            out.println("Invalid operation!!!");
                            break;
                    }
                }
                i++;
                if (i > 1){
                    i=0;
                }
            }

        } catch (IOException ex) {
            server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
