package tests;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import processing.app.tools.MovieMaker;
import processing.core.PApplet;
import processing.video.Movie;
import stwa.ScreenWriter;

public class MovieTest extends PApplet
{

  Movie movie;

  public void setup()
  {
    size(640, 360);
    background(0);

    movie = new Movie(this, "/Users/dhowe/Documents/eclipse-workspace/SomeThingWeAre/movie-test.mov");
    movie.loop();
  }

  void movieEvent(Movie m)
  {
    m.read();
  }

  public void draw()
  {
    if (movie.available() == true) {
      movie.read();
    }
    image(movie, 0, 0, width, height);
  }
  
  public static void main(String[] args)
  {
    try
    {
      
      File f = new File("/Users/dhowe/Documents/eclipse-workspace/SomeThingWeAre/src/data/frames/0000704.jpg");
      System.out.println(f.exists());
      Path sl = ScreenWriter.symLink(f, new File(f.getParentFile(), "happy.jpg"));
            System.out.println(sl.toFile().getAbsolutePath());
      f = sl.toFile();
      System.out.println(f);
      

      System.out.println(Files.isSymbolicLink(f.toPath()));
      System.out.println(Files.readSymbolicLink(f.toPath()));
      MovieMaker.imageRead(f);
      //ImageIO.read(new File("/Users/dhowe/Documents/eclipse-workspace/SomeThingWeAre/src/data/frames/0000675.jpg"));
      
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
