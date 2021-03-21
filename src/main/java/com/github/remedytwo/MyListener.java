package com.github.remedytwo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


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
            String youtube_url = content.split(" ")[1]; // le lien Youtube désigne le deuxième mot du message

            User user = message.getAuthor();
            String user_avatar_url = user.getAvatarUrl() + "?size=1024";

            File image = download(user_avatar_url);
            File music = youtubedl(youtube_url);
            File result = ffmpeg(image, music);

            channel.sendFile(result).queue();

            image.delete();
            music.delete();
            while(!result.delete()){}
        }
    }

    private File download(String image_url)
    {
        try
        {
            URL file_url = new URL(image_url);
            InputStream in = file_url.openStream();
            Files.copy(in, Paths.get("./ressources/image" + getExtension(file_url)), StandardCopyOption.REPLACE_EXISTING);
            return new File("ressources/image" + getExtension(file_url));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private File youtubedl(String url)
    {
        try
        {
            String cmd = "./lib/youtube-dl --extract-audio --audio-format mp3 -o ./ressources/\"music.%(ext)s\" " + url;
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            process.destroy();
            return new File("ressources/music.mp3");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private File ffmpeg(File image, File music)
    {
        try
        {
            String cmd = "lib/ffmpeg -loop 1 -y -i \"" + image.getAbsolutePath() + "\" -i \"" + music.getAbsolutePath() + "\" -shortest ressources/result.mp4";
            Process process = Runtime.getRuntime().exec(cmd);
            Thread.sleep(10 * 1000);
            process.destroy();
            return new File("ressources/result.mp4");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
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
}