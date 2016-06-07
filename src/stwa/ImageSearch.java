package stwa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import rita.RiTa;

public class ImageSearch extends JSoupUtil {
	
	public static boolean PRINT_NETWORK_INFO = true;
	public static boolean PRINT_CACHE_INFO = false;

	public static String QUERY_TEMPLATE = "https://www.google.com/search?q=%QUERY%&hl=en&noj=1&tbs=isz:l,ic:gray,itp:photo&tbm=isch&source=lnt&sa=X";

	protected boolean useCache;
	public Map cookies = null;

	protected static File cache;
	public static String cacheDir = ".";
	static Map methodCache = new HashMap();
	protected static boolean hasShutdownHook;
	
	public ImageSearch() {
		this(false);
	}

	public ImageSearch(boolean useCache) {
		
		SomeGenerator.log("Creating ImageSearch: useCache=" + useCache);
		this.useCache = useCache;
		if (useCache) {
			addShutdownHook();
			loadCache();
		}
	}

	public static int cacheSize() {
		return methodCache.size();
	}

	public synchronized void addShutdownHook() {
		if (hasShutdownHook)
			return;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// App.log("Inside attachShutDownHook");
				if (useCache) {

					if (cache != null && cache.exists())
						serializeCache();
					else
						SomeGenerator.warn("No cache to serialize: " + cache);
				}
			}
		});

		hasShutdownHook = true;
	}

	public void clearCache() {
		
		methodCache = new HashMap();
		
		try {
			if (cache != null && cache.exists()) {

				if (!cache.delete() || cache.exists())
					throw new RuntimeException("delete-fail");
			}
			
			SomeGenerator.log("Cache-cleared");
			
		} catch (Exception e) {
			
			SomeGenerator.log("[WARN] Unable to delete cache-file: " + cache);
		}
	}

	private static void loadCache() {
		
		String cacheFileName = cacheDir + "/cache." + ImageSearch.class.getName()+ ".ser";

		try {
			
			if (cache == null)
				cache = new File(cacheFileName);

			if (!cache.exists()) {

				if (PRINT_CACHE_INFO)
					SomeGenerator.log("Create: " + cache.getAbsolutePath());

				if (!cache.createNewFile())
					throw new RuntimeException("[WARN] Could not create cache file: "
							+ cacheFileName);

				if (cache.exists())
					serializeCache(); // empty hash
			}

			ObjectInputStream in = new ObjectInputStream(new FileInputStream(cache));
			Map cacheMap = (Map) in.readObject();
			in.close();

			SomeGenerator.log("Cache: " + cacheFileName + " (" + methodCache.size() + " entries)");
			
			int i = 0;
			for (Iterator it = cacheMap.keySet().iterator(); it.hasNext(); i++) {
				String key = (String) it.next();
				Object val = cacheMap.get(key);
				methodCache.put(key, val);
				if (PRINT_CACHE_INFO) SomeGenerator.log(i+") "+key, 4);
			}

			
		} catch (Exception e) {
			SomeGenerator.warn("Could not load/create cache file: " + cacheFileName + "\n"
					+ RiTa.stackToString(e));
		}
	}

	public static synchronized int serializeCache() {
		
		try {
			if (cache == null || !cache.exists()) {
				throw new RuntimeException("Attempt to serialize non-existent cache: "+ cache);
			}

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cache));
			out.writeObject(methodCache);
			out.flush();
			out.close();

			if (PRINT_CACHE_INFO && methodCache.size() > 0)
				SomeGenerator.log("Cache-serialized: " + methodCache.size() + " entries");

			return methodCache.size();
			
		} catch (Exception e) {
			
			System.err.println("[WARN] " + RiTa.stackToString(e));
		}

		return -1;
	}

	public File[] saveImages(String query, int maxNum) {
		
		return this.saveImages(query, maxNum, cacheDir);
	}

	public BufferedImage[] fetchImages(String query, int maxNum) {

		String[] urls = fetchURLs(query, Integer.MAX_VALUE); // one full page

		ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();

		for (int i = 0; i < urls.length; i++) {
			BufferedImage fetched = fetchImage(urls[i]); // can fail

			if (fetched != null) {

				images.add(fetched);

				if (images.size() == maxNum)
					break;
			}
		}

		if (images.size() < maxNum)
			System.err.println("Unable to fetch all " + maxNum + " images, found "
					+ images.size());

		return images.toArray(new BufferedImage[0]);
	}

	public BufferedImage fetchImage(String url) {
		BufferedImage fetched = null;
		try {
			fetched = ImageUtil.fetchImage(url);
		} catch (Exception e) {
			throw new RuntimeException("Failed on: " + url + "\n  " + e.getMessage());
		}
		return fetched;
	}

	public File[] saveImages(String query, int maxNum, String saveDir) {

		File dir = new File(saveDir);
		if (!dir.exists()) {
			if (!dir.mkdirs())
				throw new RuntimeException("Unable to create directory: " + dir);
		}

		String[] urls = fetchURLs(query, Integer.MAX_VALUE); // full page
		ArrayList<File> images = new ArrayList<File>();

		for (int i = 0; i < urls.length; i++) {
			try {
				images.add(ImageUtil.saveImage(urls[i], 
						dir.getAbsolutePath() + "/" + images.size()));
				if (images.size() == maxNum)
					break;
			} catch (Throwable e) {
				System.err.println("Unable to fetch: " + urls[i]);
			}
		}

		if (images.size() < maxNum)
			System.err.println("Unable to fetch all " +
					maxNum + " images, found " + images.size());

		return images.toArray(new File[0]);
	}

	public String[] fetchThumbnails(String query) {
		return this.fetchThumbnails(query, Integer.MAX_VALUE, false);
	}

	public String[] fetchThumbnails(String query, int maxResults) {
		return this.fetchThumbnails(query, maxResults, false);
	}

	public String[] fetchThumbnails(String query, int maxResults, boolean randomizeOrder) {
		
		String method = "fetchThumbnails", result[] = null;
		String url = QUERY_TEMPLATE.replaceAll("%QUERY%", query);

		if (useCache)
			result = (String[]) cacheFetch(method, url);

		if (result == null) { // do the fetch

			ArrayList<String> list = new ArrayList<String>();

			try {
				if (PRINT_NETWORK_INFO)
					SomeGenerator.log("[HTTP] Get: " + cacheKey(method, url));

				Document doc = fetchDocument(url);
				selectImgSrcTags(list, doc);
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (list.size() < 1) { // no exception, no result

				System.err.println("[WARN] No results for '" + query + "'");
				return new String[0];
			}

			if (randomizeOrder)
				Collections.shuffle(list);

			result = list.toArray(new String[0]);

			if (useCache && result.length > 0)
				cachePut(method, url, result);
		}

		return Arrays.copyOfRange(result, 0, Math.min(result.length, maxResults));
	}

	private void selectImgSrcTags(ArrayList<String> list, Document doc) {
		
		if (doc == null) return; 

		Elements divs = doc.select(".rg_i");

		for (Element e : divs) {
			String src = e.attr("src");
			if (src == null || src.length() < 1)
				src = e.attr("data-src");

			if (src != null && src.length() > 0) {
				list.add(src);// .replaceAll(".*?[&?]imgurl=([^&]+)&.*", "$1"));
			}
		}
	}

	public String[] fetchURLs(String query) {
		
		return fetchURLs(query, Integer.MAX_VALUE);
	}
	
	public String[] fetchURLs(String query, int maxResults) {
		
		String method = "fetchURLs", result[] = null;
		String url = QUERY_TEMPLATE.replaceAll("%QUERY%", query);

		if (useCache)
			result = (String[]) cacheFetch(method, url);

		if (result == null) { // do the fetch

			ArrayList<String> list = new ArrayList<String>();

			try {
				if (PRINT_NETWORK_INFO)
					SomeGenerator.log("Http-get: " + url);

				Document doc = fetchDocument(url);
				selectHrefs(list, doc);

			} catch (Exception e) {
				
				throw new RuntimeException(e);
			}

			result = list.toArray(new String[0]);

			if (useCache) {
				if (PRINT_NETWORK_INFO)
					SomeGenerator.log("Cache-get: " + url);
				
				cachePut(method, url, result);
			}
		}

		return Arrays.copyOfRange(result, 0, Math.min(result.length, maxResults));
	}

	private void selectHrefs(ArrayList<String> list, Document doc) {
		Elements divs = doc.select(".rg_l");
		for (Element e : divs) {
			String imgUrl = e.attr("href").replaceAll(".*?[&?]imgurl=([^&]+)&.*", "$1");
			list.add(imgUrl);
		}
	}

	private static Object cacheFetch(String method, String arg) {
		String key = cacheKey(method, arg);
		Object obj = methodCache.get(key);

		if (PRINT_CACHE_INFO) {
			String s = "Cache-" + (obj != null ? "Hit" : "Miss");
			SomeGenerator.log("" + s + ": " + key);
		}

		return obj;
	}

	private static String cacheKey(String method, String arg) {
		return method + "(" + arg + ")";
	}

	private static void cachePut(String method, String arg, Object result) {
		String key = cacheKey(method, arg);
		if (PRINT_CACHE_INFO) SomeGenerator.log("Cache-write: " + key);
		methodCache.put(key, result);
	}
	
	public ImageLoc[] fetchURLPairs(String query) {
		return fetchURLPairs(query, Integer.MAX_VALUE);
	}
	
	public ImageLoc[] fetchURLPairs(String query, int maxResults) {
		
		ImageLoc[] result = null;
		String method = "fetchURLPairs";
		String url = QUERY_TEMPLATE.replaceAll("%QUERY%", query);

		if (useCache)
			result = (ImageLoc[]) cacheFetch(method, url);

		if (result == null) { // do the fetch

			ArrayList<ImageLoc> list = new ArrayList<ImageLoc>();

			try {
				if (PRINT_NETWORK_INFO)
					SomeGenerator.log("Http-get: " + url);

				Document doc = fetchDocument(url);

				Elements divs = doc.select(".rg_l");
				
				for (Element e : divs) {
					
					String thumbUrl = null; 
					
					//TODO: is this causing our invalid URLS?
					String imgUrl = e.attr("href").replaceAll(".*?[&?]imgurl=([^&]+)&.*", "$1");
					
					Elements children = e.children();
					for (Element e2 : children) {
						
						if (e2.className().equals("rg_i")) {
							thumbUrl = e2.attr("src");
							if (thumbUrl == null || thumbUrl.length() < 1)
								thumbUrl = e2.attr("data-src");
						}
					}

					if (imgUrl != null && imgUrl.length() > 0 && 
							thumbUrl != null && thumbUrl.length() > 0 ) 
					{
						list.add(new ImageLoc(imgUrl, thumbUrl));
					}
				}

			} catch (Exception e) {
				
				throw new RuntimeException(e);
			}

			result = list.toArray(new ImageLoc[0]);

			if (useCache) {
				
				if (PRINT_NETWORK_INFO)
					SomeGenerator.log("Cache-get: " + result);
				
				cachePut(method, url, result);
			}
		}

		return Arrays.copyOfRange(result, 0, Math.min(result.length, maxResults));
	}

	public static void main(String[] args) {
		PRINT_CACHE_INFO = false;
		PRINT_NETWORK_INFO = true;

		String query = "\"something we are\" abstract";
		ImageSearch imgFetch = new ImageSearch(false);
		ImageLoc[] imgs = imgFetch.fetchURLPairs(query);
		RiTa.out(imgs);
		//String[] thumbs = imgFetch.fetchThumbnails(query);
		//RiTa.out(imgs);
		
//		String[] imgs = imgFetch.fetchURLs(query);
//		App.log("Found " + imgs.length + " images\n");
//		// if (1==1) return;
//		imgs = imgFetch.fetchURLs(query);
//		App.log("Found " + imgs.length + " images\n");
//		for (int i = 0; i < imgs.length; i++) {
//			App.log(imgs[i].substring(0, Math.min(imgs[i].length(), 100)));
//		}
		//imgFetch.clearCache();
		// PRINT_CACHE_INFO = false;
		// cacheDir = "/tmp/";
		// ImageSearch imgFetch = new ImageSearch(true);
		// String[] imgs = imgFetch.fetchThumbnails("black+dog");
		//
		// //for (int i = 0; i < imgs.length; i++) App.log(i+") "+imgs[i]);
		//
		// imgFetch.serializeCache();
		// App.log("[RESULT] "+imgs.length+" images");
		// imgFetch = new ImageSearch(false);
		// imgs = imgFetch.fetchThumbnails("black+dog");
		// App.log("[RESULT] "+imgs.length+" images");
		// imgFetch.clearCache();

		/*
		 * String url = imgs[(int) (Math.random()*imgs.length)]; App.log(url);
		 * imgFetch.saveImage(url, "/Users/dhowe/Desktop/test");
		 */
	}


}