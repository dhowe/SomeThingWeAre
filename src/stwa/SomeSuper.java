package stwa;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PImage;
import rita.RiTa;
import stwa.SomeGenerator.FrameSet;

import com.google.gson.Gson;

public class SomeSuper extends PApplet
{
  public static String dataSubFolder = ""; // get audio/phrase-counts from here

  public static String[] keywords = { "abstract", "close", "dry", "erotic", 
  	"filmic",  "geometric", "grotesque", "noir", 
  	"passive-aggressive", "psychological", "silent" };
  
  public static Map<String, int[]> colorTable = new HashMap<String, int[]>();

  public static int serverPort = 8081;
  
  static
  {
    RiTa.SILENT = true;
    
    // tints based on wikipedia article re: tinting of b/w films
    colorTable.put("amber", new int[] { 255, 191, 0, 159 }); // amber
    colorTable.put("blue", new int[] { 0, 153, 204, 159 }); // blue
    colorTable.put("green", new int[] { 0, 204, 83, 159 }); // green
    colorTable.put("lavender", new int[] { 230, 230, 250, 159 }); // lavender
    colorTable.put("red", new int[] { 204, 0, 0, 159 }); // red
    colorTable.put("rose", new int[] { 244, 194, 197, 159 }); // rose
    colorTable.put("sepia", new int[] { 144, 98, 52, 175 }); // sepia
    colorTable.put("yellow", new int[] { 255, 255, 51, 159 }); // yellow
    
    // colorTable.put("blue", new int[] { 0, 153, 204, 127 }); // blue
    // colorTable.put("green", new int[] { 0, 204, 153, 127 }); // green
    // colorTable.put("pea", new int[] { 204, 253, 0, 127 }); // pea
    // colorTable.put("purple", new int[] { 100, 0, 150, 127 }); // purple
    // colorTable.put("maroon", new int[] { 153, 0, 103, 127 }); // maroon
  }

  public static final float fps = 30;

  public static String dataDir = "src/data/"; // location of the data folder
  public static boolean disableLogs = false; // disable logging output

  public String phrase = "", title = "Some Thing We Are", audioFile;
  public String[] phrases;
  public PImage current;
  public int[] counts;
	public int duration, nextFrameMs, currentIdx, audioStartMs;
	long startMs, pauseStartMs = 0;

  // ======================================================================

  public void parsePhraseData()
  {
    String fname = dataSubFolder + "phraseCounts.json";
    log("Loading phrase data from: "+dataDir+fname);
    String json = RiTa.loadString(fname);
    FrameSet[] frames = new Gson().fromJson(json, FrameSet[].class);
    
    int totalFrames = 0;
    this.phrases = new String[frames.length];
    this.counts = new int[frames.length];
    for (int i = 0; i < frames.length; i++)
    {
      if (i != frames[i].index)
        throw new RuntimeException("Bad data in JSON: " + frames[i]);
      phrases[i] = frames[i].text.trim();
      counts[i] = frames[i].frames;
      totalFrames += frames[i].frames; 
    }
    
    this.duration = framesToTime(totalFrames);
  }

	protected static int framesToTime(int numFrames) {
		
		return (int) ((numFrames/fps) * 1000);
	}
	
  public float movieDuration() {
  	
  	return this.duration;
  }
  
  public int movieTime() {
  	
  	int t = (int) (System.currentTimeMillis() - startMs);
  	if (pauseStartMs > 0)
  		t -= (System.currentTimeMillis() - pauseStartMs);
  	return t;
  }
  
  public float playhead() {
  	
  	return min(1, movieTime()/movieDuration());
  }
  
  protected void drawText(String s)
  {
  	if (s.indexOf(' ') < 0) return; // no single words
  	
    textSize(60);
    textAlign(CENTER);
    fill(0);
    text(s, width / 2, height - 30);
    fill(255);
    text(s, width / 2 - 1, height - 31);
  }

  protected String randomColor()
  {
    Object[] keys = colorTable.keySet().toArray();
    return (String) keys[(int) random(keys.length)];
  }

  protected String randomKeyword()
  {
    String keyword = keywords[(int) random(keywords.length)];
    // System.err.println("Random keyword: "+keyword);
    return keyword;
  }

  protected String createPath(String dir, String keyword)
  {
    return dir + '_' + keyword;
  }

	public static String thumbnailName(String thumbnail) {
		
		if (thumbnail == null || thumbnail.length()<1)
				return null;
		String[] parts = thumbnail.split("tbn:");
		if (parts.length != 2) {
			warn("Unable to parse thumbnailUrl: "+thumbnail);
			return null;
		}
		return parts[1] + ScreenWriter.DOT + ScreenWriter.imgExt;
	}
	
  // ---------------------------- Simple logging ----------------------------

  public static void warn(String s)
  {
    log(System.err, s, 2);
  }

  public static void log(String s)
  {
    log(System.out, s, 2);
  }

  public static void warn(String s, int indent)
  {
    log(System.err, "[WARN] " + s, indent);
  }

  public static void log(String s, int indent)
  {
    log(System.out, s, indent);
  }

  public static void log(PrintStream pw, String s, int indent)
  {
    String in = "";
    for (int i = 0; i < indent; i++)
      in += " ";
    if (!disableLogs)
      pw.println(in + s);
  }

}
