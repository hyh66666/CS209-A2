package cn.edu.sustech.cs209.chatting.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class test {
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        InputStreamReader ins = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(ins);
        String string = input.next();
        String str = br.readLine();
        String str1 = br.readLine();
        System.out.println("string data: "+string);
        System.out.println("str data: "+str);
        System.out.println("str1 data: "+str1);
    }
    static class User  {
        String name;
        public User(String name){
            this.name=name;
        }
    }
}
