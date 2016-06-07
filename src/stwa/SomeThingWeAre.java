package stwa;

import rita.RiTa;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PImage;
import stwa.SomeGenerator.FrameSet;

import com.google.gson.Gson;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

/*
 * TODO:
 * 	show selection of specific image (need to make sure its in the top 2-3 rows)
 * 	gifs / videos
 * 	ambient sound
 * allow users to take their movie on usb? 
 */
public class SomeThingWeAre extends PApplet {
	
  public static final boolean DBUG = false;
  public static final boolean USE_SERVER = true;

	public static String[] keywords = getKeywords();  
  public static Map<String, int[]> colorTable;

	int duration, countIndex, tintColor[], counts[];
	long nextframe = Long.MAX_VALUE, moviestart;
	String keyword, color, phrase, phrases[];
	
	AudioPlayer audio;
	PImage frames[];
	File frameDir;
	SearchResult[] searches;
	SomeServerWeAre server;

	public void setup() {
		
  	if (DBUG)
  		size(1050, 540);
  	else
  		size(1920, 1000);
		
		audio = new Minim(this).loadFile("audio.mp3");  
		
		if (USE_SERVER)
			server = new SomeServerWeAre(this, 8081);
		
		parsePhraseData();
		
		background(0);
		
		startMovie("abstract", "red", false);
	}
	
	public void draw() {
		
		if (System.currentTimeMillis() >= nextframe)
			advanceFrame();
		
		if (DBUG) showInfo();
	}
	
	// called in 3 cases: start,loop,msg-from-ui
	void startMovie(String keyword, String color, boolean reuseData) {
		
		this.audio.pause();
		this.color = color;
		this.keyword = keyword;
		this.frameDir = new File("src/data/frames_"+keyword);
		this.tintColor = getTint(); 
		this.countIndex = 0;
		this.phrase = phrases[countIndex];
		
		if (!reuseData) loadData();
		
		image(frames[0], 0, 0, width, height); // first frame
		
		moviestart = System.currentTimeMillis();

		setNext();
	}

	private void setNext() {
		
		if (countIndex == 1) {
			audio.rewind();
			audio.play();
		}
		nextframe = System.currentTimeMillis() + framesToTime(counts[countIndex]);
		//System.out.println("frame "+countIndex+" in "+(nextframe-System.currentTimeMillis()+"ms"));
	}

  private void advanceFrame() {
  	
  	nextframe = Long.MAX_VALUE;
  	
		if (++countIndex == counts.length) { 
			startMovie(this.keyword, this.color, true);
		}
		
		phrase = phrases[countIndex];
		
		background(0);
		drawImage();		
		drawText(phrase);
		sendToServer();
		setNext();
	}

	private void drawImage() {
		if (frames[countIndex] != null) {
			if (phrase.indexOf(' ') > -1) // no tint for title/credits
				tint(tintColor[0], tintColor[1], tintColor[2], tintColor[3]);
			image(frames[countIndex], 0, 0, width, height);
		}
	}

  private void sendToServer() {
  	
  	if (server != null) { // who send searches to the clients
			
			if (countIndex < searches.length && searches[countIndex] != null)
				server.sendSearchResult(searches[countIndex]); // TODO: send elapsed-time?
			else
				System.err.println("No search-result file found for index: "+countIndex);
		}
	}

	protected void drawText(String s)
  {
  	 // no single words
  	if (s == null || s.indexOf(' ') < 0) return;
  	
    textSize(60);
    textAlign(CENTER);
    fill(0);
    text(s, width / 2, height - 30);
    fill(255);
    text(s, width / 2 - 1, height - 31);
  }
  
	public float position()
  {
		return (System.currentTimeMillis()-moviestart) / (float)duration;
  }
	
	public String toElapsed(int ms)
  {		
		int seconds = ms / 1000;
		long s = seconds % 60, m = (seconds / 60) % 60;
		return String.format("%02d:%02d", m, s);
  }
	
	public void parsePhraseData()
  {
    String json = RiTa.loadString("phraseCounts.json");
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
  
	protected int framesToTime(int numFrames) {
		
		return (int) ((numFrames/30.0) * 1000); // 30 fps
	}
  
	private void showInfo() {
		
		textAlign(RIGHT);
		textSize(16);
		fill(40);
		rect(width, 0, -90, height);
		fill(255);
		text(countIndex, width-10, 30);
		text(toElapsed((int) (position()*duration)), width-10, 50);
		text(toElapsed(duration), width-10, 70);
		text(this.keyword, width-10, 90);
		text(this.color, width-10, 110);
		fill(200);
		rect(width, height-5, position()*-width, 5);
	}
	
	public String totalTime() {
		
		return toElapsed(duration);
	}
	
	public void loadData() {
		
		long ts = System.currentTimeMillis();
				
	  // load the frame files
	  final String[] imgFiles = frameDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".jpg");
		}});
	  
	  String[] jsFiles = frameDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".js");
		}});
	  
	  // validate our phrase data against the files
	  if (imgFiles.length != phrases.length) {
	  	throw new RuntimeException("files.length != phrases.length " + 
	  			imgFiles.length +" != "+phrases.length);
	  }
	  
	  loadImages(imgFiles, 10);
	  loadJSONSearches(imgFiles, jsFiles);
	  
	  System.out.println("Loaded initial data in "+(System.currentTimeMillis()-ts)+"ms");
	}

	private void loadImages(final String[] imgFiles, final int initialImagesToLoad) {
			  
	  frames = new PImage[imgFiles.length];
	  for (int i = 0; i < Math.min(initialImagesToLoad, imgFiles.length); i++) {
			frames[i] = loadImage(frameDir+"/"+imgFiles[i]);
	  }
	  	  
	  Thread thread = new Thread() {
	  	public void run() {
			  for (int i = initialImagesToLoad; i < imgFiles.length; i++) {
					frames[i] = loadImage(frameDir+"/"+imgFiles[i]);
			  }
		  	System.out.println("Loader thread completed...");
	  	}
	  };
	  
	  thread.setPriority(Thread.MIN_PRIORITY);
	  thread.start();
	}
	
	public void loadJSONSearches(String[] imgFiles, String[] jsFiles) {
		
		Gson gson = new Gson();
		searches = new SearchResult[jsFiles.length];
		for (int i = 0; i < jsFiles.length; i++) {
			String json = RiTa.loadString(frameDir + "/" + jsFiles[i]);
			searches[i] = gson.fromJson(json, SearchResult.class);
			searches[i].localizeThumbnails("img/cache/"); // local thumbs
		}
		
		// validate search-results data against the files
		if (imgFiles.length != searches.length) {

			System.out.println("Files.length != search-results.length " +
					imgFiles.length +" != "+searches.length);
		}
	}

	private static String[] getKeywords() {
		return new String[] { "abstract", "close", "dry", "erotic", 
	  	"filmic",  "geometric", "grotesque", "noir", 
	  	"passive-aggressive", "psychological", "silent" };
	}
	
	private int[] getTint() {
		if (colorTable == null) {
			colorTable = new HashMap<String, int[]>();
			colorTable.put("amber", new int[] { 255, 191, 0, 159 }); // amber
			colorTable.put("blue", new int[] { 0, 153, 204, 159 }); // blue
			colorTable.put("green", new int[] { 0, 204, 83, 159 }); // green
			colorTable.put("lavender", new int[] { 230, 230, 250, 159 }); // lavender
			colorTable.put("red", new int[] { 204, 0, 0, 159 }); // red
			colorTable.put("rose", new int[] { 244, 194, 197, 159 }); // rose
			colorTable.put("sepia", new int[] { 144, 98, 52, 175 }); // sepia
			colorTable.put("yellow", new int[] { 255, 255, 51, 159 }); // yellow
		}
		return colorTable.get(this.color);
	}
	
	public static void main(String[] args) {
		PApplet.main(SomeThingWeAre.class.getName());
	}
}
