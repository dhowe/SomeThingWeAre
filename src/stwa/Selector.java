package stwa;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

import com.google.gson.GsonBuilder;

public abstract class Selector
{
  protected ImageSearch search;
  protected ScreenWriter parent;

  public Selector(ImageSearch imageSearch) {
  	
		this.search = imageSearch;
	}

	public abstract BufferedImage selectFrame(String query, int frameNum);
  
	protected BufferedImage selectRandom(String[] urls, SearchResult sresult) {
		
		int tries = 0, maxTries = 5;
		BufferedImage select = null;
		String currentUrl = null;
		
		while (select == null) {

			if (urls.length < 1) {
				SomeGenerator.log("Failed to select random after " + tries + " tries");
				return null;
			}

			int imgIdx = (int) (Math.random() * urls.length);
			
			SomeGenerator.log("Trying url #" + imgIdx + " of " + urls.length+"...");

			try {

				if (++tries >= maxTries) {
					SomeSuper.warn("Giving up after "+maxTries +" tries");
					return null;
				}
				currentUrl = urls[imgIdx];
				select = search.fetchImage(currentUrl); // can fail
			
			} catch (Throwable e) {
				
				select = null;
				currentUrl = null;
				urls = removeElement(urls, imgIdx);
				SomeSuper.warn(e.getMessage());
				SomeSuper.log(urls.length + " remaining to try");
			}
		}
		
		int i = 0;
		List<ImageLoc> images = sresult.getImages();
		for (Iterator<ImageLoc> it = images.iterator(); it.hasNext();i++) {
			ImageLoc next = it.next();
			if (next.url.equals(currentUrl))
				sresult.setSelectedIndex(i);
		}
		
		return select;
	}

	public static String[] removeElement(String[] a, int removeIndex) {
		
		if (a.length<1 || removeIndex<0 || removeIndex >= a.length)
			return a;
		
		String[] result = new String[a.length-1];
		System.arraycopy(a, removeIndex + 1, result, removeIndex, a.length - 1 - removeIndex);
		
		return result;
	}
		
	public static void main(String[] args) {
		String[] urls = { "http://a","http://b", };
		new FrameSelector(new ImageSearch(false)).selectRandom(urls,null);
	}
}
