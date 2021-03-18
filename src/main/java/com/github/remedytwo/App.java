package com.github.remedytwo;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class App
{
    static String token;
    public static void main(String[] args) 
    {
        try
        {
            JDA jda = JDABuilder.createDefault(token).build();
        }
        catch (LoginException e)
        {
            e.printStackTrace();
        }
    }
}
