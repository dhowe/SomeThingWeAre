package stwa;

import java.awt.image.BufferedImage;

import processing.core.PApplet;

/*
 * NEXT:
 * 	1) Pick random movie and color on double-click, then test problem with start-movie
 * 	2) Fix jumpy slider...
 *  3) Fade-out audio instead of stopping it ?
 */
public class SomeGenerator extends SomeSuper
{
  public static boolean fetchFrames = true;  // search if true, otherwise just composite
  public static boolean writeSymLinks = true; // saves space on disk by not recreating frames
  public static boolean cacheSearches = true;  // use previous images search results
	
  String ccDir = dataDir+"cc", imageDir = dataDir+"frames",
	  audioFile = dataDir + dataSubFolder + "audio.mp3";

  ScreenWriter screenWriter;
  boolean finished;
  
  public void setup()
  {
    size(960, 600);

    this.parsePhraseData();

    String keyword = "filmic";
    
    screenWriter = new ScreenWriter(this)
    	.selector(new FrameSelector(cacheSearches))
    	.frameDir(createPath(imageDir, keyword))
      .closedClassDir(ccDir)
      .keyword(keyword)
      .phrases(phrases)
      .frameCounts(counts)
      .soundtrack(audioFile);
    
    if (fetchFrames)
      screenWriter.generate(true);
    
    background(0);
  }
  

	public void draw()
  {
    if (current != null) {
      background(0);
      image(current, 0, 0, width, height);//current.width/2, current.height/2);
      drawText(phrase);
    }
    else {
    	drawText("loading...");
    }
  }
  
  // called from ScreenWriter.generate -> ScreenWriter.fireSelectionEvent
  public void onSelection(int phraseIdx, BufferedImage selectedImage)
  {
    current = ImageUtil.toPImage(selectedImage);
    phrase = phrases[phraseIdx];
  }

  class FrameSet {
  	int index, frames;
  	String text;
  	public String toString() {
  		return "{ index: "+index+", text: "+text+", frames: "+frames+" }";
  	}
  }

  protected void drawText(String s)
  {
    textSize(32);
    textAlign(CENTER);
    fill(0);
    text(s, width / 2, height - 30);
    fill(255);
    text(s, width / 2-1, height - 31);
  }

  public static void main(String[] args) // run as application
  {
  	PApplet.main(SomeGenerator.class.getName());
  }
}
 