package stwa.util;

import java.io.File;
import java.io.FilenameFilter;

import rita.RiTa;
import stwa.ImageUtil;
import stwa.ScreenWriter;
import stwa.SearchResult;
import stwa.SomeSuper;

import com.google.gson.Gson;

public class CachePopulator {
	
	public static void main(String[] args) {
    
		String[] keywords = SomeSuper.keywords; // all keywords
		
		for (int k = 0; k < keywords.length; k++) {
		
			File frameDir = new File("src/data/frames_"+keywords[k]);
			String[] jsFiles = frameDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".js");
			}});
			
	    // load the js files
	    Gson gson = new Gson();
	    SearchResult[] searches = new SearchResult[jsFiles.length];
	    for (int i = 0; i < jsFiles.length; i++) {
	    	String json = RiTa.loadString(frameDir + "/" + jsFiles[i]);
	    	searches[i] = gson.fromJson(json, SearchResult.class);
	    }
	    
	    System.out.println("Found "+searches.length +" thumbnail files");
	    
	    int count=0, wcount=0;
	    for (int i = 0; i < searches.length; i++) {
	    	
				String[] thumbnails = searches[i].getThumbnails();
				int len = Math.min(30,thumbnails.length);
				for (int j = 0; j < len; j++) {
					
					if (thumbnails[j] == null || thumbnails[j].length() < 1 ) {
						System.err.println("Skipping empty thumbnail-name: #"+j);
						continue;
					}
					
					String fn = SomeSuper.thumbnailName(thumbnails[j]);
					if (fn == null || fn.length() <1 ) {
						System.err.println("Skipping empty thumbnail: #"+j);
						continue;
					}
					
					count++;
					System.out.println("File "+ i + "/"+searches.length+"("+keywords[k]+
							"), Thumbnail "+j+"/"+len+",  "+thumbnails[j]);
					
					File out = new File("www/img/cache/"+fn);
					if (out.exists()) {
						System.out.println("  File already exists...");
						continue;
					}
					
					if (ImageUtil.saveImage(thumbnails[j], out, ScreenWriter.imgExt)) { 
						System.out.println("  Wrote "+out.getAbsolutePath()+"\n");
						wcount++;
					}
				}
			}
	    System.out.println("\nProcessed "+count+" thumbnails, wrote "+wcount+" new cache files");
		}
	}

}
