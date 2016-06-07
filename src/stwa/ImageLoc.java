package stwa;

import java.io.Serializable;

public class ImageLoc implements Serializable {
	
	public String url, thumbUrl;
	
	public ImageLoc() {}
	
	public ImageLoc(String url, String thumbUrl) {
		this.url = url;
		this.thumbUrl = thumbUrl;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getThumbUrl() {
		return thumbUrl;
	}
	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}
	@Override
	public String toString() {
		return url+" "+thumbUrl;
	}
	public boolean isHttp() {
		return url.startsWith("http") && thumbUrl.startsWith("http");
	}
}