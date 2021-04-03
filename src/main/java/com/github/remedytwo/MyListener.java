package com.github.remedytwo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
        switch (message.getContentRaw().split(" ")[0])
        {
            case ("!help"):
                event.getChannel().sendMessage
                (
                    "`!theme musiclink (@mention) (time)`\n" +
                    "`!meme message1 message2`\n" +
                    "`!montage videolink musiclink`"
                );
                break;

            case ("!meme"):
                commandMeme(message);
                break;

            case("!montage"):
                commandMontage(message);
                break;

            case("!theme"):
                commandTheme(message);
                break;
        }
        emptyFolder(new File("tmp/"));
        logger.info("Files deleted");
    }

    private void commandMeme(Message message)
    {
        logger.info("Command found: \"" + message.getContentRaw() + "\"");
            try
            {
                message.getChannel().sendTyping().queue();

                String text_a = message.getContentRaw().split(";")[1];
                text_a = formatText(text_a.split(" "));
                PrintWriter pw_a = new PrintWriter("tmp/text1.txt");
                pw_a.println(text_a);
                pw_a.close();
                logger.info("First text set : " + text_a);
                String text_b = message.getContentRaw().split(";")[2];
                text_b = formatText(text_b.split(" "));
                PrintWriter pw_b = new PrintWriter("tmp/text2.txt");
                pw_b.println(text_b);
                pw_b.close();
                logger.info("Second text set : " + text_b);

                try
                {
                    String[] cmd = {"tools/ffmpeg.exe", "-i", "resources/meme.mp4", "-vf", 
                    "\"[in]drawtext=fontfile=resources/Consolas.ttf:", "textfile=tmp/text1.txt:", "fontcolor=black:", "fontsize=30:", "x=(w/4)-(text_w/2):", "y=(h-text_h)/2/2/2/2,", 
                    "drawtext=fontfile=resources/Consolas.ttf:", "textfile=tmp/text2.txt:", "fontcolor=black:", "fontsize=30:", "x=((w/2)+(w/4))-(text_w/2):", "y=(h-text_h)/2/2/2/2[out]\"", 
                    "-codec:a", "copy", "-y", "tmp/final.mp4"};

                    File meme = launchCommand(cmd, new File("tmp/final.mp4"));
                    logger.info("Sending video...");
                    message.getChannel().sendMessage(message.getAuthor().getAsMention()).addFile(meme).queue();
                    logger.info("Video sent");
                    logger.info("Deleting files...");
                    logger.info("Files deleted");
                }
                catch (IllegalArgumentException e)
                {
                    logger.debug(e.toString());
                    message.getChannel().sendMessage("Bug.").queue();
                }
            }
            catch (FileNotFoundException e)
            {
                logger.debug(e.toString());
                message.getChannel().sendMessage("Bug.").queue();
            }
    }

    private void commandMontage(Message message)
    {
        logger.info("Command found: \"" + message.getContentRaw() + "\"");
        message.getChannel().sendTyping().queue();

        String videoURL = message.getContentRaw().split(" ")[1];
        String musicURL = message.getContentRaw().split(" ")[2];
        int[] videoSeek = getYoutubeSeek(videoURL);
        int[] musicSeek = getYoutubeSeek(musicURL);
        File videoFile = new File("tmp/video.mp4");
        File musicFile = new File("tmp/music.mp3");
        String[] videoCommand = {"tools/youtube-dl.exe", "-fbestvideo", "-o", "\"" + videoFile.getPath() + "\"", videoURL};
        String[] musicCommand = {"tools/youtube-dl.exe", "--extract-audio", "--audio-format", "mp3", "-o", "\"" + musicFile.getPath() + "\"", musicURL};
        videoFile = launchCommand(videoCommand, videoFile);
        musicFile = launchCommand(musicCommand, musicFile);
        File output = new File("tmp/output.mp4");
        String[] mixCommand = {"tools/ffmpeg.exe", 
        "-ss", String.format("%02d", videoSeek[0]) + ":" + String.format("%02d", videoSeek[1]) + ":" + String.format("%02d", videoSeek[2]), "-i", videoFile.getPath(), 
        "-ss", String.format("%02d", musicSeek[0]) + ":" + String.format("%02d", musicSeek[1]) + ":" + String.format("%02d", musicSeek[2]), "-i", musicFile.getPath(), 
        "-vcodec", "copy", "-acodec", "copy", "-map", "0:0", "-map", "1:0", output.getPath()};
        output = launchCommand(mixCommand, output);

        message.getChannel().sendFile(output).queue();
    }

    private void commandTheme(Message message)
    {
        User user = message.getAuthor();
        logger.info("Command found: \"" + message.getContentRaw() + "\"");
        message.getChannel().sendTyping().queue();
        String[] message_array = message.getContentRaw().split(" ");
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
            String[] youtubedl_cmd = {"tools/youtube-dl.exe", "--extract-audio", "--audio-format", "mp3", "-o", "tmp/\"music.mp3\"", music_link};
            File music = launchCommand(youtubedl_cmd, new File("tmp/music.mp3"));
            File image = download(user_avatar_url);

            String[] cmd = getCommandFromType(image, music, seek);
            File result = launchCommand(cmd, new File("tmp/result.mp4"));
            
            logger.info("Sending video...");
            message.getChannel().sendMessage(user.getAsMention()).addFile(result, user.getName() + "'s_theme.mp4").queue();
            logger.info("Video sent");
        }
        catch (IllegalArgumentException e)
        {
            message.getChannel().sendMessage("La vidéo est trop lourde.").queue();
        }
    }

    private File download(String image_url)
    {
        try
        {
            URL file_url = new URL(image_url);
            logger.info("Downloading avatar...");
            InputStream in = file_url.openStream();
            Files.copy(in, Paths.get("tmp/image" + getExtension(file_url)), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Avatar downloaded");
            return new File("tmp/image" + getExtension(file_url));
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        return null;
    }

    private File launchCommand(String[] command, File resultat)
    {
        logger.info(arrayToString(command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = null;
        try
        {
            process = pb.start();
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine())
            {
                String ligne = scanner.nextLine();
                logger.info(ligne);
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
        return resultat;
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
        if (url.contains("?t="))
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
        else
        {
            int[] time = {0, 0, 0};
            return time;
        }
    }

    private String[] getCommandFromType(File image, File music, int[] seek)
    {
        if (image.getName().contains(".png")) // pour les png
        {
            // output : tools/ffmpeg.exe -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"tools/ffmpeg.exe", "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "tmp/result.mp4"};
        }
        else if (image.getName().contains(".gif")) // pour les gifs
        {
            // output : tools/ffmpeg.exe -ignore_loop 0 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -crf 40 -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"tools/ffmpeg.exe", "-ignore_loop", "0", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "40", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "tmp/result.mp4"};
        }
        else if (image.getName().contains(".jpg") || image.getName().contains(".jpeg")) // pour les jpg
        {
            // output : tools/ffmpeg.exe -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:a copy -shortest -y -strict -2 result.mp4
            return new String[]{"tools/ffmpeg.exe", "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", "tmp/result.mp4"};
        }
        return null;
    }

    private String formatText(String[] tab)
    {
        int compte = 0;
        String result = "";
        for(int i = 0; i < tab.length; i++)
        {
            compte+= tab[i].length();
            if(compte >= 15)
            {
                tab[i] += "\n";
                compte = 0;
            }
            result += tab[i] + " ";
        }
        return result;
    }
}