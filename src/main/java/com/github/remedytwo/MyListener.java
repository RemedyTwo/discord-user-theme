package com.github.remedytwo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.swing.plaf.multi.MultiInternalFrameUI;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MyListener extends ListenerAdapter 
{
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot()) return;
        // We don't want to respond to other bot accounts, including ourself
        Message message = event.getMessage();
        String content = message.getContentRaw(); 
        // getContentRaw() is an atomic getter
        // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)
        if (content.equals("!ping"))
        {
            MessageChannel channel = event.getChannel();
            channel.sendMessage("Pong!").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
        }
        if (content.equals("!avatar"))
        {
            MessageChannel channel = event.getChannel();
            User user = message.getAuthor();
            String user_id = user.getAvatarUrl() + "?size=1024";
            channel.sendMessage(user_id).queue();
            download(user_id);
        }
        if (content.contains("!theme"))
        {
            MessageChannel channel = event.getChannel();
            String youtube_url = content.split(" ")[1];

            User user = message.getAuthor();
            String user_avatar_url = user.getAvatarUrl() + "?size=1024";
            String user_avatar_filename = download(user_avatar_url);

            youtubedl(youtube_url);
/*             while (!music.exists())
            {

            }
            ffmpeg(user_avatar_filename, ".\\ressources\\music.mp3");
            while (!result.exists())
            {

            }

            channel.sendFile(new File(".\\ressources\\result.mp4")); */
        }
    }

    private String download(String image_url)
    {
        try
        {
            URL file_url = new URL(image_url);
            InputStream in = file_url.openStream();
            Files.copy(in, Paths.get(".\\ressources\\image" + getExtension(file_url)), StandardCopyOption.REPLACE_EXISTING);
            return ".\\ressources\\image" + getExtension(file_url);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    private String getExtension(URL file_url)
    {
        int i = String.valueOf(file_url).length() - 1;
        while(String.valueOf(file_url).charAt(i) != '.')
        {
            i--;
        }
        return String.valueOf(file_url).substring(i, String.valueOf(file_url).length() - 10);
    }

    public void ffmpeg(String image_path, String music_path)
    {
        try
        {
            String cmd = "ffmpeg -loop 1 -y -i " + image_path + " -i " + music_path + " -shortest .\\ressources\\result.mp4";
            Process process = Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void youtubedl(String url)
    {
        try
        {
            String cmd = "youtube-dl --extract-audio --audio-format mp3 -o .\\ressources\\\"music.%(ext)s\" " + url;
            Process process = Runtime.getRuntime().exec(cmd);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}