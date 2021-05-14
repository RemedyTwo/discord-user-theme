package com.github.remedytwo;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ffmpeg 
{
    private final File ffmpeg = new File("tools/ffmpeg.exe");
    private final File[] input;
    private final String command;
    private final File output;

    final Logger logger = LoggerFactory.getLogger(ffmpeg.class);

    public ffmpeg (File[] input, String command, File output) 
    {
        this.input = input;
        this.command = command;
        this.output = output;
    }

    private String[] buildCommand ()
    {
        int tmp = 0;

        String fullCommand[] = new String[1 + (2 * this.input.length) + this.command.split(" ").length + 1];
        fullCommand[0] = ffmpeg.getPath();
        for (int i = 0 ; i < this.input.length ; i++)
        {
            fullCommand[tmp + i] = "-i";
            fullCommand[tmp + i + 1] = this.input[i].getPath();
            tmp++;
        }
        fullCommand[fullCommand.length - 1] = this.output.getPath();
        return fullCommand;
    }

    public File launch ()
    {
        return launchCommand(buildCommand());
    }

    private File launchCommand(String[] command)
    {
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
        }
        catch (IOException e)
        {
            logger.debug(e.toString());
        }
        return new File (command[command.length - 1]);
    }
}
