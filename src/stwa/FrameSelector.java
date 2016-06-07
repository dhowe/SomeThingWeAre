package stwa;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;

public class FrameSelector extends Selector
{
	public FrameSelector(ImageSearch imageSearch) {
		super(imageSearch);
	}
	
	public FrameSelector(boolean cacheImages) {
		this(new ImageSearch(cacheImages));
	}

	public BufferedImage selectFrame(String phrase, int frameNum) {

		BufferedImage selected = fetchRandom(phrase, frameNum, true);
		if (selected == null) {
			SomeSuper.log("Trying without keyword...");
			selected = fetchRandom(phrase, frameNum, false);
			if (selected == null) { // still no luck?
				String placeHolder = toPlaceHolder(phrase);
				SomeSuper.log("Trying placeholder: "+placeHolder+"...");
				try {
					selected = parent.loadCcFrame(placeHolder);
				} catch (Exception e) {
					SomeSuper.warn("Failed: "+e.getMessage());
					selected = parent.loadCcFrame("black");
				}
			}
		}
		return selected;
  }
	
  private String toPlaceHolder(String phrase) {
  	if (phrase.indexOf(' ') < -1) return phrase;
		String[] words = phrase.split(" ");
		return words[0] + words[words.length-1] + (int)parent.parent.random(5);	
	}

	protected SearchResult toSearchResult(int frameNum, String phrase, ImageLoc[] pairs)
  {
    SearchResult iresults = new SearchResult(frameNum, parent.keyword);
    iresults.query = phrase.replaceAll("\\+", " ");
    //image.status = status;
    
    if (pairs != null) {
    	for (int i = 0; i < pairs.length; i++) {
    		if (pairs[i].url.length()>0 && pairs[i].thumbUrl.length()>0)
    			iresults.addImage(pairs[i].url, pairs[i].thumbUrl);
    	}
    }
    
    return iresults;
  }
	
	protected BufferedImage fetchRandom(String phrase, int frameNum, boolean addKeyword) {

		String query = parent.prepareQuery(phrase, addKeyword);
		
		SomeGenerator.log("Searching: " + query);

		int tries = 0, maxTries = 3;
		
		// TODO: change to ImageLoc objects...
		//String[] urls = null;
		ImageLoc[] urls = null;
		SearchResult searchResult = null;
		
		while (urls == null && tries++ < maxTries) {
			
			try {
				// TODO: change to fetchUrlPairs
				//urls = search.fetchURLs(query, 100);
				urls = search.fetchURLPairs(query, 100);
				
				
				// TODO: must be moved after select 
				//SearchResult ir = toImageResult(frameNum, phrase, urls);

				searchResult = toSearchResult(frameNum, phrase, urls);
				SomeGenerator.log("ImageResult.frameNum="+searchResult.frameIndex);
				
			} catch (Exception e1) {
				
				SomeGenerator.warn("fetchRandom failed("+tries+"/" + 
						maxTries+"): "+e1.getMessage());
				
				if (e1 instanceof java.net.SocketTimeoutException) {
					try {
						
						Thread.sleep(3000);
						
					} catch (InterruptedException e) {					}
				}
			}
		}
		
		//List<String> valids = new ArrayList<String>();
		List<ImageLoc> valids = new ArrayList<ImageLoc>();
		for (int i = 0; i < urls.length; i++) {
			
			if (urls[i].isHttp()) valids.add(urls[i]);
		}
		
		int skipped = urls.length - valids.size();
		if (skipped > 0)
			SomeGenerator.log("Ignoring: " + skipped +" embedded images");
		
		//urls = valids.toArray(new String[0]);
		urls = valids.toArray(new ImageLoc[0]);
		
 		if (urls.length == 0) {
			
			SomeGenerator.warn("Failed to fetch batch after " + tries + " tries");
			return null;
		}
		
		SomeSuper.log("Found " + urls.length + " valid images");

		// TODO: update search result with selected image...
		String[] candidates = new String[urls.length];
		for (int i = 0; i < urls.length; i++) {
			candidates[i] = urls[i].url;
		}
		
		BufferedImage selected = selectRandom(candidates, searchResult);
		
		parent.writeSearchResults(searchResult);

		return selected;
	}
	
}
