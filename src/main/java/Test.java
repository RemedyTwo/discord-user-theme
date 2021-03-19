import java.io.IOException;

public class Test 
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        String image_path = "C:\\Users\\sbilal\\Desktop\\ffmpeg-test\\image.jpg";
        String music_path = "C:\\Users\\sbilal\\Desktop\\ffmpeg-test\\music.mp3";
        command(image_path, music_path);
    }

    public static void command(String image_path, String music_path) throws IOException, InterruptedException
    {
        String cmd = "ffmpeg -loop 1 -y -i " + image_path + " -i " + music_path + " -shortest .\\ressources\\result.mp4";
        Process process = Runtime.getRuntime().exec(cmd);
    }
}
