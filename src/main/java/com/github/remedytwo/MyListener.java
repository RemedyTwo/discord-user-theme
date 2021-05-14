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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MyListener extends ListenerAdapter 
{
    private final File averageFan = new File("resources/AverageFan.mp4");
    private final File vibingCat = new File ("resources/VibingCat.mp4");

    private final File Consolas = new File("resources/Consolas.ttf");

    private final File ffmpeg = new File("tools/ffmpeg.exe");
    private final File ffprobe = new File("tools/ffprobe.exe");
    private final File youtubedl = new File("tools/youtube-dl.exe");

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
                logger.info("Command found: \"" + message.getContentRaw() + "\"");
                event.getChannel().sendMessage
                (
                    "`!theme musiclink (@mention) (time)`\n" +
                    "`!meme message1 message2`\n" +
                    "`!montage videolink musiclink`"
                );
                break;

            case ("!meme"):
                logger.info("Command found: \"" + message.getContentRaw() + "\"");
                commandMeme(message);
                break;

            case ("!montage"):
                logger.info("Command found: \"" + message.getContentRaw() + "\"");
                commandMontage(message);
                break;

            case ("!theme"):
                logger.info("Command found: \"" + message.getContentRaw() + "\"");
                commandTheme(message);
                break;

            case ("!vibe"):
                logger.info("Command found: \"" + message.getContentRaw() + "\"");
                commandVibe(message);
                break;
        }
        emptyFolder(new File("tmp/"));
        logger.info("Files deleted");
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

    private void commandMeme(Message message)
    {
        message.getChannel().sendTyping().queue();
        try
        {
            String contenu = message.getContentRaw().replace("!meme ", "");
            String text_a = contenu.split(";")[0];
            text_a = formatText(text_a.split(" "));
            PrintWriter pw_a = new PrintWriter("tmp/text1.txt");
            pw_a.println(text_a);
            pw_a.close();
            logger.info("First text set : " + text_a);
            String text_b = contenu.split(";")[1];
            text_b = formatText(text_b.split(" "));
            PrintWriter pw_b = new PrintWriter("tmp/text2.txt");
            pw_b.println(text_b);
            pw_b.close();
            logger.info("Second text set : " + text_b);

            try
            {
                File meme = new File("tmp/final.mp4");
                String[] cmd = {ffmpeg.getPath(), "-i", averageFan.getPath(), "-vf", 
                "\"[in]drawtext=fontfile=" + Consolas.getPath() + ":", "textfile=tmp/text1.txt:", "fontcolor=black:", "fontsize=30:", "x=(w/4)-(text_w/2):", "y=(h-text_h)/2/2/2/2,", 
                "drawtext=fontfile=" + Consolas.getPath() + ":", "textfile=tmp/text2.txt:", "fontcolor=black:", "fontsize=30:", "x=((w/2)+(w/4))-(text_w/2):", "y=(h-text_h)/2/2/2/2[out]\"", 
                "-codec:a", "copy", "-y", meme.getPath()};

                launchCommand(cmd);
                logger.info("Sending video...");
                message.getChannel().sendMessage(message.getAuthor().getAsMention()).addFile(meme).queue();
                logger.info("Video sent");
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

    private void commandMontage(Message message)
    {
        message.getChannel().sendTyping().queue();

        String videoURL = message.getContentRaw().split(" ")[1];
        String musicURL = message.getContentRaw().split(" ")[2];
        int[] videoSeek = getYoutubeSeek(videoURL);
        int[] musicSeek = getYoutubeSeek(musicURL);
        File videoFile = new File("tmp/video.mp4");
        File musicFile = new File("tmp/music.mp3");
        downloadVideo(videoFile, videoURL);
        downloadAudio(musicFile, musicURL);
        File output = new File("tmp/output.mp4");
        String[] mixCommand = {ffmpeg.getPath(), 
        "-ss", String.format("%02d", videoSeek[0]) + ":" + String.format("%02d", videoSeek[1]) + ":" + String.format("%02d", videoSeek[2]), "-i", videoFile.getPath(), 
        "-ss", String.format("%02d", musicSeek[0]) + ":" + String.format("%02d", musicSeek[1]) + ":" + String.format("%02d", musicSeek[2]), "-i", musicFile.getPath(), 
        "-vcodec", "copy", "-acodec", "copy", "-map", "0:0", "-map", "1:0", "-shortest", output.getPath()};
        launchCommand(mixCommand);

        if (output.length() > 8000000)
        {
            reduceVideoUnder8Mo(output);
        }

        message.getChannel().sendFile(output).queue();
    }

    private void commandTheme(Message message)
    {
        User user = message.getAuthor();
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

        try
        {
            File image = downloadAvatar(user_avatar_url);

            File music = new File("tmp/music.mp3");
            downloadAudio(music, music_link);

            File resultat = new File("tmp/result.mp4");
            String[] cmd = {};
            if (image.getName().contains(".png")) // pour les png
            {
                // output : tools/ffmpeg.exe -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -c:a copy -shortest -y -strict -2 result.mp4
                cmd = new String[]{ffmpeg.getPath(), "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", resultat.getPath()};
            }
            else if (image.getName().contains(".gif")) // pour les gifs
            {
                // output : tools/ffmpeg.exe -ignore_loop 0 -i imagepath -ss time -i musicpath -c:v libx264 -pix_fmt yuv420p -crf 40 -c:a copy -shortest -y -strict -2 result.mp4
                cmd = new String[]{ffmpeg.getPath(), "-ignore_loop", "0", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "40", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", resultat.getPath()};
            }
            else if (image.getName().contains(".jpg") || image.getName().contains(".jpeg")) // pour les jpg
            {
                // output : tools/ffmpeg.exe -loop 1 -f image2 -r 1 -i imagepath -ss time -i musicpath -c:a copy -shortest -y -strict -2 result.mp4
                cmd = new String[]{ffmpeg.getPath(), "-loop", "1", "-f", "image2", "-r", "1", "-i", "\"" + image.getPath() + "\"", "-ss", String.format("%02d", seek[0]) + ":" + String.format("%02d", seek[1]) + ":" + String.format("%02d", seek[2]), "-i", "\"" + music.getPath() + "\"", "-c:a", "copy", "-shortest", "-y", "-strict", "-2", resultat.getPath()};
            }
            launchCommand(cmd);
            
            logger.info("Sending video...");
            message.getChannel().sendMessage(user.getAsMention()).addFile(resultat, user.getName() + "'s_theme.mp4").queue();
            logger.info("Video sent");
        }
        catch (IllegalArgumentException e)
        {
            message.getChannel().sendMessage("La vidéo est trop lourde.").queue();
        }
    }

    private void commandVibe(Message message)
    {
        String videoURL = message.getContentRaw().split(" ")[1];
        File videoFile = new File("tmp/video.mp4");
        downloadVideoAndAudio(videoFile, videoURL);

        File output = new File("tmp/final.mp4");
        overlayGreenScreen(videoFile, vibingCat, output);
        if (output.length() > 8000000)
        {
            reduceVideoUnder8Mo(output);
        }
        logger.info("Sending video...");
        message.getChannel().sendFile(output).queue();
        logger.info("Video sent");
    }

    private String launchCommand(String[] command)
    {
        logger.info(arrayToString(command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = null;
        try
        {
            process = pb.start();
            Scanner scanner = new Scanner(process.getInputStream());
            String out = "";
            while (scanner.hasNextLine())
            {
                String ligne = scanner.nextLine();
                logger.info(ligne);
                out += ligne;
            }
            try
            {
                process.waitFor();
            }
            catch (InterruptedException e)
            {
                process.destroy();
            }
            scanner.close();
            return out;
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        return "";
    }

    private String downloadVideo(File file, String url)
    {
        String[] command = {youtubedl.getPath(), "-fbestvideo", "-o", file.getPath(), url};
        return launchCommand(command);
    }

    private String downloadAudio(File file, String url)
    {
        String[] command = {youtubedl.getPath(), "--extract-audio", "--audio-format", "mp3", "-o", file.getPath(), url};
        return launchCommand(command);
    }

    private String downloadVideoAndAudio(File file, String url)
    {
        String[] command = {youtubedl.getPath(), "-fbest", "-o", file.getPath(), url};
        return launchCommand(command);
    }

    private String overlayGreenScreen(File input, File overlay, File output)
    {
        String[] command = {ffmpeg.getPath(), "-y", "-i", input.getPath(), "-i", overlay.getPath(), "-filter_complex", "\"[1:v]reverse[r];[1:v][r]concat,loop=" + (getVideoLength(input)/12-1) + ":360,setpts=N/30/TB[loop],[loop]scale=" + getVideoResolution(input) + "[ovrl],[ovrl]colorkey=0x00D700:0.5:0[ckout];[0:v][ckout]overlay[out]\"", "-map", "\"[out]\"", "-map", "0:a", output.getPath()};
        return launchCommand(command);
    }
    
    private void reduceVideoUnder8Mo(File video)
    {
        String[] first_command = {ffmpeg.getPath(), "-y", "-i", video.getPath(), "-c:v", "libx264", "-b:v", String.valueOf(getBitrateUnder8Mo(video)) + "ka", "-pass", "1", "-an", "-f", "null", "NUL"};
        launchCommand(first_command);
        String[] second_command = {ffmpeg.getPath(), "-i", video.getPath(), "-c:v", "libx264", "-b:v", String.valueOf(getBitrateUnder8Mo(video)) + "k", "-pass", "2", "-c:a", "aac", "-b:a", "128k", "output.mp4"};
        launchCommand(second_command);
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

    private File downloadAvatar(String image_url)
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

    private String getExtension(URL file_url)
    {
        int i = String.valueOf(file_url).length() - 1;
        while(String.valueOf(file_url).charAt(i) != '.')
        {
            i--;
        }
        return String.valueOf(file_url).substring(i, String.valueOf(file_url).length() - 10);
    }

    private int getBitrateUnder8Mo(File video)
    {
        return 8000/getVideoLength(video);
    }
    
    private int getVideoLength(File video)
    {
        String[] command = {ffprobe.getPath(), "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", video.getPath()};
        String out = launchCommand(command);
        return Double.valueOf(out).intValue() + 1;
    }

    private String getVideoResolution(File video)
    {
        String[] command = {ffprobe.getPath(), "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height", "-of", "csv=s=x:p=0", video.getPath()};
        return launchCommand(command).replace("x", ":");
    }
}