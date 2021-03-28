package com.github.remedytwo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
        if (user.isBot()) return;
        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();
        if (content.equals("!help"))
        {
            channel.sendMessage("`!theme musiclink (@mention) (time)`\n!meme;message1;message2").queue();
        }
        else if (content.split(" ")[0].contains("!meme"))
        {
            logger.info("Command found: \"" + content + "\"");
            try
            {
                channel.sendTyping().queue();

                String text_a = content.split(";")[1];
                PrintWriter pw_a = new PrintWriter("resources/bin/text1.txt");
                pw_a.println(text_a);
                pw_a.close();
                logger.info("First text set : " + text_a);

                String text_b = content.split(";")[2];
                PrintWriter pw_b = new PrintWriter("resources/bin/text2.txt");
                pw_b.println(text_b);
                pw_b.close();
                logger.info("Second text set : " + text_b);

                String[] cmd = {"lib/ffmpeg.exe", "-i", "resources/meme/meme.mp4", "-vf", 
                "\"[in]drawtext=fontfile=resources/meme/arial.ttf:", "textfile=resources/bin/text1.txt:", "fontcolor=black:", "fontsize=30:", "x=(w/4)-(text_w/2):", "y=(h-text_h)/2/2/2/2,", 
                "drawtext=fontfile=resources/meme/arial.ttf:", "textfile=resources/bin/text2.txt:", "fontcolor=black:", "fontsize=30:", "x=((w/2)+(w/4))-(text_w/2):", "y=(h-text_h)/2/2/2/2[out]\"", 
                "-codec:a", "copy", "-y", "resources/bin/final.mp4"};
                try
                {
                    File meme = ffmpeg(cmd, new File("resources/bin/final.mp4"));
                    logger.info("Sending video...");
                    channel.sendMessage(user.getAsMention()).addFile(meme).queue();
                    logger.info("Video sent");
                    logger.info("Deleting files...");
                    emptyFolder(new File("resources/bin/"));
                    logger.info("Files deleted");
                }
                catch (IllegalArgumentException e)
                {
                    logger.debug(e.toString());
                    channel.sendMessage("Bug.").queue();
                }
            }
            catch (FileNotFoundException e)
            {
                logger.debug(e.toString());
                channel.sendMessage("Bug.").queue();
            }
        }
        else if (content.split(" ")[0].contains("!theme"))
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

                String[] cmd = getCommandFromType(image, music, seek);
                File result = ffmpeg(cmd, new File("resources/bin/result.mp4"));
                
                logger.info("Sending video...");
                channel.sendMessage(user.getAsMention()).addFile(result, user.getName() + "'s_theme.mp4").queue();
                logger.info("Video sent");

                logger.info("Deleting files...");
                emptyFolder(new File("resources/bin/"));
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
    }

    private File download(String image_url)
    {
        try
        {
            URL file_url = new URL(image_url);
            logger.info("Downloading avatar...");
            InputStream in = file_url.openStream();
            Files.copy(in, Paths.get("resources/bin/image" + getExtension(file_url)), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Avatar downloaded");
            return new File("resources/bin/image" + getExtension(file_url));
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        return null;
    }

    private File youtubedl(String url) throws IllegalArgumentException, MalformedURLException
    {
        String[] cmd = {"lib/youtube-dl", "--extract-audio", "--audio-format", "mp3", "-o", "resources/bin/\"music.mp3\"", url};
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
        File file = new File("resources/bin/music.mp3");
        if (file.length() > 8000000 || file.length() == 0)
        {
            logger.info("Music is too heavy (" + file.length() + ")");
            throw new IllegalArgumentException();
        }
        logger.info("Music downloaded");
        return file;
    }

    private File ffmpeg(String[] cmd, File result) throws IllegalArgumentException
    {
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
        if (result.length() > 8000000 || result.length() == 0)
        {
            logger.info("Assembled video is too heavy (" + result.length() + ")");
            throw new IllegalArgumentException();
        }
        logger.info("Video assembled");
        return result;
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
        while(folder.listFiles().length != 0)
        {
            for(File file: folder.listFiles())
            {
                if (!file.isDirectory()) 
                {
                    file.delete();
                }
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

    private String[] getCommandFromType(File image, File music, int[] seek)
    {
        if (image.getName().contains(".png")) // pour les png
        {
            // output : lib/ffmpeg -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"lib/ffmpeg", "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "resources/bin/result.mp4"};
        }
        else if (image.getName().contains(".gif")) // pour les gifs
        {
            // output : lib/ffmpeg -ignore_loop 0 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -crf 40 -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"lib/ffmpeg", "-ignore_loop", "0", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "40", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "resources/bin/result.mp4"};
        }
        else if (image.getName().contains(".jpg") || image.getName().contains(".jpeg")) // pour les jpg
        {
            // output : lib/ffmpeg -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"lib/ffmpeg", "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "resources/bin/result.mp4"};
        }
        return null;
    }
}