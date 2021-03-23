package com.github.remedytwo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class App
{
    public static void main(String[] args) throws LoginException, FileNotFoundException, IOException 
    {
        Scanner scanner = new Scanner(new File("token.txt"));
        String token = scanner.nextLine();
        JDA jda = JDABuilder.createDefault(token).build();
        jda.addEventListener(new MyListener());
    }
}
