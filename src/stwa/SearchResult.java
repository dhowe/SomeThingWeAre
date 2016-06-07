package stwa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SearchResult
{	
  public String status, query, resultCount, keyword;
	public List<ImageLoc> images;
	public int frameIndex;
	public int selectedIndex;
	public float position;
	

	public SearchResult() {
  	this(-1, null);
	}
	
	public SearchResult(int frameNum, String keyword) {
  	
  	this.frameIndex = frameNum;
  	this.keyword = keyword;
  	this.images = new ArrayList<ImageLoc>();  	
  	//this.thumbnails = new ArrayList<String>();  	
	}
  
  public String getQuery() {
		return query;
	}

  public String[] getThumbnails() {

		List<String> l = new ArrayList<String>();
		for (Iterator it = images.iterator(); it.hasNext();) {
			ImageLoc loc = (ImageLoc) it.next();
			String thumb = loc.thumbUrl;
			if (thumb != null && thumb.length() > 0)
				l.add(thumb);
		}
		return l.toArray(new String[0]);
	}
  
	public void localizeThumbnails(String path) {
		for (Iterator it = images.iterator(); it.hasNext();) {
			ImageLoc loc = (ImageLoc) it.next();
			String thumb = SomeSuper.thumbnailName(loc.thumbUrl);
			if (thumb != null && thumb.length() > 0)
				loc.thumbUrl = path + thumb;
		}
	}

	public void setQuery(String query) {
		this.query = query;
	}

  public List<ImageLoc> getImages() {
		return images;
	}

	public void setImages(List<ImageLoc> images) {
		this.images = images;
	}

	public void addImage(String imageUrl, String thumbnailUrl)
  {
    images.add(new ImageLoc(imageUrl, thumbnailUrl));
  }
	
	public String toJSON() {
		
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting().serializeNulls();
    Gson gson = builder.create();
    return gson.toJson(this);
	}

	public int getFrameIndex() {
		return frameIndex;
	}

	public void setFrameIndex(int frameIdx) {
		this.frameIndex = frameIdx;
	}
	
	public int getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(int selectedIndex) {
		this.selectedIndex = selectedIndex;
	}
	
	@Override
	public String toString() {
		return toJSON();
	}
	
  public float getPosition() {
		return position;
	}

	public void setPosition(float position) {
		this.position = position;
	}
	
  public static void main(String[] args)
  {
    SearchResult result = new SearchResult(24, "bouncy");

    result.status = "ok";
    result.query = "other nations. According, therefore, as this";
    result.resultCount = "&nbsp;Results <b>1</b> - <b>20</b> of about <b>25,500,000</b> for <b>other nations. According therefore as this</b>. (<b>0.22</b> seconds)&nbsp;";
    result.addImage("http://t0.gstatic.com/images?q=tbn:mfc4D8hIqwLDsM:http://www.shirkatgah.org/header-images/TWc9PQ%3D%3D.jpg","http://t2.gstatic.com/images?q=tbn:i_Tu7ksmWR52IM:http://image50.webshots.com/150/6/88/58/430668858aqMXfU_fs.jpg");

    System.out.println(result.toJSON());
  }
}
