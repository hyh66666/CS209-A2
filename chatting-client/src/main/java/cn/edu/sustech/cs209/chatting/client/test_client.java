package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class test_client {
    public static void main(String[] args) throws IOException {
        final int Port = 1234;
        Scanner input = new Scanner(System.in);
        try (Socket socket = new Socket("localhost", Port)) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            Scanner in = new Scanner(inputStream);
            PrintWriter out = new PrintWriter(outputStream, true);
            new Thread(() -> {
                String response;
                while ((response = in.nextLine()) != null) {
                    System.out.println(response);
                }
            }).start();
//            init(input, out);
            String sender = input.next();
            out.println(sender);
            while (true) {
                String type = input.next();
                String receiver = input.next();
                String text = input.next();
                String total = type+" "+receiver+"\n"+text;
                System.out.println("Sender: " + text);
                out.println(total);
                if (type.equals("quit")) {
                    break;
                }
            }

        }
    }
    public static synchronized void init(Scanner input, PrintWriter out) {
        String sender = input.next();
        out.println(sender);
    }
}
