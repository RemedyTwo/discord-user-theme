package com.github.remedytwo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MyListener extends ListenerAdapter 
{
    final Logger logger = LoggerFactory.getLogger(MyListener.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        User user = event.getAuthor();
        if (user.isBot()) return; // We don't want to respond to other bot accounts, including ourself
        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();
        // getContentRaw() is an atomic getter
        // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)
        if (content.equals("!help"))
        {
            channel.sendMessage("`!theme musiclink (@mention) (time)`").queue(); // Important to call .queue() on the RestAction returned by sendMessage(...)
        }
        if (content.split(" ")[0].contains("!theme"))
        {
            if (content.split(" ").length > 1)
            {
                logger.info("Command found: \"" + content + "\"");
                channel.sendTyping().queue();
                String[] message_array = content.split(" ");
                String music_link = "";
                int[] seek = {0, 0, 0};
                for(int i = 1; i < message_array.length; i++)
                {
                    switch(i)
                    {
                        case(1):
                            music_link = message_array[i];
                            logger.info("Music URL set : " + music_link);
                            if (music_link.contains("?t="))
                            {
                                seek = getYoutubeSeek(music_link);
                            }
                            break;
                        case(2):
                            user = message.getMentionedUsers().get(0);
                            logger.info("Tagged user set : " + user.getName());
                            break;
                        case(3):
                            for(int j = 0; j < message_array[i].split(":").length; j++)
                            {
                                seek[j] = Integer.parseInt(message_array[3].split(":")[j]); // [0] seconds [1] minutes [2] hours
                            }
                            logger.info("Seeked time set : " + seek[0] + ":" + seek[1] + ":" + seek[2]);
                    }   
                }

                String user_avatar_url = user.getAvatarUrl() + "?size=1024";

                message.delete().queue();

                try
                {
                    File music = youtubedl(music_link);
                    File image = download(user_avatar_url);
                    File result = ffmpeg(image, music, seek);
                    
                    logger.info("Sending video...");
                    channel.sendFile(result).queue();
                    logger.info("Video sent");

                    logger.info("Deleting files...");
                    while(music.exists() || image.exists() || result.exists())
                    {
                        emptyFolder(new File("ressources/"));
                    }
                    logger.info("Files deleted");
                }
                catch (IllegalArgumentException e)
                {
                    channel.sendMessage("La vidéo est trop lourde.").queue();
                }
                catch (MalformedURLException e)
                {
                    channel.sendMessage("L'URL n'est pas supporté.").queue();
                }
            }
            else
            {
                channel.sendMessage("Lien manquant.").queue();
            }
        }
    }

    private File download(String image_url)
    {
        try
        {
            URL file_url = new URL(image_url);
            logger.info("Downloading avatar...");
            InputStream in = file_url.openStream();
            Files.copy(in, Paths.get("ressources/image" + getExtension(file_url)), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Avatar downloaded");
            return new File("ressources/image" + getExtension(file_url));
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        return null;
    }

    private File youtubedl(String url) throws IllegalArgumentException, MalformedURLException
    {
        String[] cmd = {"lib/youtube-dl", "--extract-audio", "--audio-format", "mp3", "-o", "ressources/\"music.mp3\"", url};
        logger.info(arrayToString(cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = null;
        logger.info("Downloading music...");
        try
        {
            process = pb.start();
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine())
            {
                String ligne = scanner.nextLine();
                logger.debug(ligne);
                if (ligne.contains("ERROR"))
                {
                    scanner.close();
                    throw new MalformedURLException();
                }
            }
            try
            {
                process.waitFor();
            }
            catch (InterruptedException e)
            {
                process.destroy();
            }
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        File file = new File("ressources/music.mp3");
        if (file.length() > 8000000 || file.length() == 0)
        {
            logger.info("Music is too heavy (" + file.length() + ")");
            throw new IllegalArgumentException();
        }
        logger.info("Music downloaded");
        return file;
    }

    private File ffmpeg(File image, File music, int[] seek) throws IllegalArgumentException
    {
        String[] cmd = {""};
        if (image.getName().contains(".gif")) //TODO: il y a peut-être une manière plus efficace de check le type du fichier
        {
            cmd = new String[]{"ffmpeg", "-ignore_loop", "0", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-crf", "40", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "ressources/result.mp4"};
        }
        else if (image.getName().contains(".png"))
        {
            cmd = new String[]{"lib/ffmpeg", "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "ressources/result.mp4"};
        }
        logger.info(arrayToString(cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = null;
        logger.info("Assembling video...");
        try
        {
            process = pb.start();
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine())
            {
                logger.debug(scanner.nextLine());
            }
            try
            {
                process.waitFor();
            }
            catch (InterruptedException e)
            {
                process.destroy();
                logger.debug(e.toString());
            }
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        File file = new File("ressources/result.mp4");
        if (file.length() > 8000000 || file.length() == 0)
        {
            logger.info("Assembled video is too heavy (" + file.length() + ")");
            throw new IllegalArgumentException();
        }
        logger.info("Video assembled");
        return file;
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

    private void emptyFolder(File folder)
    {
        for(File file: folder.listFiles())
        {
            if (!file.isDirectory()) 
            {
                file.delete();
            }
        }
    }

    private String arrayToString(String[] array)
    {
        String string = "";
        for(int i = 0; i < array.length; i++)
        {
            string += array[i] + " ";
        }
        return string;
    }

    // exemple : https://youtu.be/zldORaRtdMw?t=205
    private int[] getYoutubeSeek(String url)
    {
        int[] time = {0, 0, Integer.parseInt(url.split(Pattern.quote("?t="))[1])}; // [0] heure [1] minute [2] seconde
        for(int i = 1; i < time.length; i++)
        {
            while(time[i] >= 60)
            {
                time[i] -= 60;
                time[i - 1] += 1;
            }
        }
        return time;
    }
}