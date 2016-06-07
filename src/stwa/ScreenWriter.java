package stwa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import processing.core.PApplet;
import rita.RiTa;

public class ScreenWriter {

	public static boolean UNIQUE_OUTPUT_NAME = true;

	public static final String SL = "/", DOT = ".", MOVIE_EXT = ".mov", imgExt = "jpg";

	String phrases[], frameDir, userDir, ccDir, keyword;

	File soundFile;
	PApplet parent;
	FrameSelector selector;
	boolean createMissingFrames, resizeImages = true;
	int frameCursor = 0, padLength = 6, frameCounts[]; 
	int targetWidth = 1920, targetHeight = 1080;
	
	public ScreenWriter(PApplet p) {

		this.parent = p;
		this.userDir = System.getProperty("user.dir");
	}

	public ScreenWriter generate(boolean runInSeparateThread) {

		if (runInSeparateThread) {

			new Thread() {
				public void run() {
					generate();
				}
			}.start();

		} else {

			generate();
		}

		return this;
	}

	public void generate() {

		File outFile = new File(this.frameDir);
		if (!outFile.exists()) {

			if (!outFile.mkdirs())
				throw new RuntimeException("Can't create/find: " + outFile);

		} else {

			SomeGenerator.log("Cleaning: " + outFile);
			
			for (File file : outFile.listFiles()) {
				file.delete();
			}
			
			if (outFile.listFiles().length > 0)
				throw new RuntimeException("Can't clean dir:  " + outFile);
		}

		SomeGenerator.log("Writing frames to: " + frameDir);

		for (int i = 0; i < phrases.length; i++) {

			BufferedImage selected = null;
			String phrase = phrases[i].toLowerCase();

			SomeGenerator.log(i + " ---------------------- ", 0);

			if (phrase.indexOf(' ') < 0) { // not a Closed-class word

				if (selector != null) { // use the selector to fetch the image

					selected = selector.selectFrame(phrase, frameCursor); // called on RandomSelector
				}

				fireSelectionEvent(parent, i, selected); // calls back to applet.onSelection

			} else { // Closed-class word

				selected = loadCcFrame(phrase);
			}

			writeMovieFrame(selected, frameCursor);
			
			frameCursor += frameCounts[i];
		}

		SomeSuper.log("\nGeneration complete, frames written to "+frameDir,0);
		
		parent.exit();
	}

	public void fireSelectionEvent(Object listener, int phraseIndex, BufferedImage bi) {

		RiTa.invoke(listener, "onSelection", new Object[] { phraseIndex, bi });
	}

	protected BufferedImage loadCcFrame(String phrase) {

		SomeGenerator.log("Loading " + phrase + DOT + imgExt);
		
		File ccFile = new File(userDir + SL + ccDir + SL + phrase + DOT + imgExt);
		if (!ccFile.exists())
			throw new RuntimeException("Unable to find: " + ccFile);

		try {
			return ImageIO.read(ccFile);

		} catch (Exception e) {

			throw new RuntimeException(e);
		}
	}
	
	protected File writeSearchResults(SearchResult sr) {

		File fout = new File(frameDir + SL + padIndex(sr.frameIndex) + DOT + "js");

		SomeGenerator.log("Writing JSON to: " + fout.getAbsolutePath());

		try {
			writeFile(fout, sr.toJSON());

			if (!fout.exists())
				throw new RuntimeException("Unable to save: " + fout);

		} catch (Exception e) {

			e.printStackTrace();
		}

		return fout;
	}		
	
  public boolean writeFile(File afile, String contents) 
  {
    try
    {
      FileWriter fw = new FileWriter(afile); 
      fw.write(contents);
      fw.flush();
      fw.close();
      return true;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  } 

	File writeMovieFrame(BufferedImage b, int idx) {

		BufferedImage img = b;
		
		if (resizeImages) {
			img = ImageUtil.resizeExact(img, targetWidth, targetHeight);
		}

		File fout = new File(frameDir + SL + padIndex(idx) + DOT + imgExt);

		SomeGenerator.log("Writing to: " + fout.getAbsolutePath());

		try {
			ImageIO.write(img, imgExt, fout);

			if (!fout.exists())
				throw new RuntimeException("Unable to save: " + fout);

		} catch (IOException e) {

			e.printStackTrace();
		}

		return fout;
	}

	public String prepareQuery(String phrase, boolean appendKeyword) {
		
		String query = "\"" + 
				phrase.trim()
				.replaceAll("^‘", "")
				.replaceAll("’$", "") + "\"";
		if (appendKeyword) query += " " + keyword;
		return query;
	}

	protected String padIndex(int idx) {

		String paddedIdx = "0" + idx;
		while (paddedIdx.length() <= padLength)
			paddedIdx = "0" + paddedIdx;
		
		return paddedIdx;
	}

	public static Path symLink(File existing, File linkName) {
		try {

			return Files.createSymbolicLink(linkName.toPath(), existing.toPath());

		} catch (Exception x) {

			throw new RuntimeException(x);
		}
	}

	public static void copyFile(File sourceFile, File destFile) {

		try {
			if (!destFile.exists()) {
				destFile.createNewFile();
			}

			FileChannel source = null;
			FileChannel destination = null;

			try {

				source = new FileInputStream(sourceFile).getChannel();
				destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {

				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ScreenWriter frameDir(String imageDir) {
		this.frameDir = imageDir;
		return this;
	}

	public String closedClassDir() {
		return this.ccDir;
	}

	public ScreenWriter closedClassDir(String ccDir) {
		this.ccDir = ccDir;
		return this;
	}

	public ScreenWriter soundtrack(String audioFile) {
		this.soundFile = new File(audioFile);
		return this;
	}

	public ScreenWriter frameCounts(int[] t) {
		this.frameCounts = t;
		return this;
	}

	public ScreenWriter phrases(String[] p) {
		this.phrases = p;
		return this;
	}

	public Selector selector() {
		return this.selector;
	}

	public ScreenWriter selector(FrameSelector selector) {
		selector.parent = this;
		this.selector = selector;
		return this;
	}

	public ScreenWriter dimensions(FrameSelector selector) {
		selector.parent = this;
		this.selector = selector;
		return this;
	}

	public ScreenWriter dimensions(int movieW, int movieH) {
		this.targetWidth = movieW;
		this.targetHeight = movieH;
		return this;
	}

	public String keyword() {
		return this.keyword;
	}

	public ScreenWriter keyword(String kw) {
		this.keyword = kw;
		return this;
	}

}