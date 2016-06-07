package stwa;

import java.io.File;
import java.io.FilenameFilter;

import processing.core.PApplet;
import processing.core.PImage;
import rita.RiTa;

import com.google.gson.Gson;

import ddf.minim.AudioPlayer;
import ddf.minim.Minim;

public class SomePlayer extends SomeSuper
{	
	public static boolean DBUG = true;
	public static boolean USE_SERVER = false;
	public static boolean MUTE_AUDIO = false;
	public static boolean USE_THUMBNAIL_CACHE = true;
	
	Minim minim;
	PImage[] frames;
	SearchResult[] searches;
	AudioPlayer audio;
	SomeServer server;
	String keyword, colorName, phrase = "";
	
	boolean audioStarted, playbackPaused;
	int[] tintColor;
	File frameDir;
		
  public void setup()
  {
  	if (DBUG)
  		size(1050, 540);
  	else
  		size(1920, 1000);

		minim = new Minim(this);
		
		parsePhraseData();
				
		if (USE_SERVER)
			server = new SomeServer(this, 8081);
		
    startMovie("abstract", "rose");  
    
    background(0);
  }

	public void draw()
  {
		if (!playbackPaused)
			updatePlayerState();
		
		if (DBUG) showInfo();
  }

	private void showInfo() {
		
		textAlign(RIGHT);
		textSize(16);
		fill(0);
		rect(width, 0, -90, height);
		fill(50);
		rect(width, height, -90, playhead()*-height);
		fill(255);
		text(movieTime(), width-10, 30);
		text("p="+currentIdx, width-10, 50);
		text(this.keyword, width-10, 70);
		text(this.colorName, width-10, 90);
		if (playbackPaused) text("paused", width-10, 110);
	}

  public void keyReleased() {
  	if (key == 'p')
  		togglePause();
  	if (key == 's')
  		startMovie("palliative", "red");
  }
	
  public int elapsedMs() { 
  	return (int) (System.currentTimeMillis() - startMs);
  }
  
  public boolean togglePause() {
		
		if (audio != null) {
			if (!playbackPaused) {
				audio.pause();
				pauseStartMs = System.currentTimeMillis();
			}
			else {
				startMs += (System.currentTimeMillis() - pauseStartMs);
				pauseStartMs = 0;
				audio.play();
			}
		}
		playbackPaused = !playbackPaused;
		
		return playbackPaused;
  }
	
	protected void updatePlayerState() {
		
		if (elapsedMs() >= nextFrameMs) { // trigger next frame
			
			nextFrameMs = Integer.MAX_VALUE; // no doubles
			
			if (currentIdx == frames.length-1) { // loop
				
				startMovie(this.frameDir, this.tintColor, false);
			}
    	
			currentIdx++;               // update the phrase index
			phrase = phrases[currentIdx]; // update the phrase
    	current = frames[currentIdx];  // update the current image

    	//log("Frame#"+currentIdx+" @ "+nextFrameMs);
    	
    	// if we are on the first non-title frame, start audio
    	if (!audioStarted && elapsedMs() >= audioStartMs) {
    		startAudio();
    	}
    	
    	background(0);
    	if (phrase.indexOf(' ') > -1) { // no tint for title/credits
    		
    		tint(tintColor[0],tintColor[1],tintColor[2],tintColor[3]);
    	}
    	
    	if (current != null)
    		if (DBUG)
    			image(current, 0, 0, 960, 540);
    		else
    			image(current, 0, 0, 1920, 1080);
    	
    	if (phrase != null)
    		drawText(phrase);
      
    	// if we have a multi-word phrase, show it
    	if (phrases[currentIdx].indexOf(' ') > 0) 
    	{
    		if (server != null) { // and send searches to the clients
    			
    			if (currentIdx < searches.length && searches[currentIdx] != null)
    				server.sendSearchResult(searches[currentIdx]); // TODO: send elapsed-time?
    			else
    				warn("No search-result file found for index: "+currentIdx);
    		}
    	}
    	
    	nextFrameMs = elapsedMs() + framesToTime(counts[currentIdx]); // update next time to switch
    }
	}

	protected void startAudio() {
		
		audioStarted = true;
		audio = minim.loadFile(dataSubFolder+"audio.mp3");  
		audio.play();
		log("Starting audio @ "+elapsedMs());
		if (MUTE_AUDIO) audio.setGain(-1000);
	}

	public void startMovie(String keyword, String colorName) {

		log("SomePlayer.startMovie("+keyword+","+colorName+")");
		
		this.keyword = keyword;
		this.colorName = colorName;
		
		int[] col = colorTable.get(colorName);
		if (col == null) {
			warn("Bad color param: "+colorName+"! Choosing random...", 0);
			col = colorTable.get(this.colorName = randomColor());
		}
		
		File frames = new File(dataDir + createPath("frames", keyword));
		if (!frames.exists()) {
			frames = new File(dataDir + createPath("frames", "abstract"));
			warn("Bad keyword param: "+keyword+"! Using abstract...", 0);
			if (!frames.exists()) 
				throw new RuntimeException("Failed to find frames in "+frames);
		}
		
		startMovie(frames, col, true);
	}
	
	public void startMovie(File fdir, int[] colors, boolean loadData) {
	
    // close the player until the audio cue
    if (audio != null) audio.close();
		
    current = null;
		frameDir = fdir;
		tintColor = colors;
		currentIdx = -1;
		nextFrameMs = 0;
		audioStarted = false;
		
		// only reload this stuff on generate, not on a loop
		
		if (loadData) {
			
	    // load the frame files
	    String[] imgFiles = frameDir.list(new FilenameFilter() {
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
	    
	    // load the image files
	    frames = new PImage[imgFiles.length];
	    for (int i = 0; i < imgFiles.length; i++) {
				frames[i] = loadImage(frameDir+"/"+imgFiles[i]);
	    }
	    
	    loadJSONSearches(imgFiles, jsFiles);
		}
    
		startMs = System.currentTimeMillis();
		log("Starting movie @ "+elapsedMs());
		audioStartMs = framesToTime(counts[0]);
		playbackPaused = false;
  }

	public void loadJSONSearches(String[] imgFiles, String[] jsFiles) {
		// load the js files
		Gson gson = new Gson();
		searches = new SearchResult[jsFiles.length];
		for (int i = 0; i < jsFiles.length; i++) {
			String json = RiTa.loadString(frameDir + "/" + jsFiles[i]);
			searches[i] = gson.fromJson(json, SearchResult.class);
			if (USE_THUMBNAIL_CACHE)
				searches[i].localizeThumbnails("img/cache/");
		}
		
		// validate search-results data against the files
		if (imgFiles.length != searches.length) {

			// not quite sure why this diff is ever more than 2
			log("Files.length != search-results.length " +
					imgFiles.length +" != "+searches.length);
		}
	}
	
  public static void main(String[] args)
  {
  	if (args != null && args.length > 0) {
  		serverPort = Integer.parseInt(args[0]);
  		log("Setting serverPort=" + serverPort);
  	}
  	
  	PApplet.main(SomePlayer.class.getName());
  }
}
 
