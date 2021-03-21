package com.github.remedytwo;

import java.io.IOException;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class App
{
    static String token = "";
    public static void main(String[] args) throws LoginException, IOException 
    {
        JDA jda = JDABuilder.createDefault(token).build();
        jda.addEventListener(new MyListener());

    }
}
