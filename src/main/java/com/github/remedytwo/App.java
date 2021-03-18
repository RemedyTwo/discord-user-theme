package com.github.remedytwo;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class App
{
    static String token = "MjA4MjU3NzUzMjI2MDE4ODE2.V5oxJQ.qUypfHAmMBmkxgL7_S67DjFnv7w";
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
