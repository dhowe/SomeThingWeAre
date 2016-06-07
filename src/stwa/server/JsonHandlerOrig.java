package stwa.server;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import rita.RiTa;
import stwa.ImageFetcher;
import stwa.search.ImageSearch;

import com.google.gson.GsonBuilder;

public class JsonHandlerOrig
{
	public static final String INFO = "[INFO] ", WARN = "[WARN] ", DELIM = " :: ";
	
	public static boolean USE_IMG_CACHE = false;
	public static int CACHE_WRITE_MS = 5 * 60 * 1000; // 5 min
  
  protected boolean networkUp = true;
  protected ImageFetcher imgSearch; 
  protected long lastCacheWrite;
  private int lastCacheSize;

	private boolean PRINT_NETWORK_INFO;
    
  public JsonHandlerOrig(ImageFetcher search)
  {
    this(search, "/genpage.js");
  }
  
  public JsonHandlerOrig(ImageFetcher search, String outputFile)
  {
    imgSearch = search;
    
    // startNetWatchThread(); this causes crashes with joup
    
    if (USE_IMG_CACHE)
      startCacheSyncThread();
  }

  private void startNetWatchThread()
  {
    System.out.println("[INFO] Starting net-watcher thread");
    
    new Thread() {
      
      public void run() {
        
        while (true) {
 
          checkNetwork();

          try
          {
            sleep(2000);
          }
          catch (InterruptedException e) {}
        }
      }

      private void checkNetwork()
      {
        String host = "http://google.com"; 
        InetSocketAddress socketAddress = new InetSocketAddress(host, 80); 
        try
        {
          SocketChannel channel = SocketChannel.open(); 
          channel.configureBlocking(false); 
          channel.connect(socketAddress);
          if (!networkUp)
            System.out.println("Network-up");
          networkUp = true;
        }
        catch (Throwable e)
        {
        	System.err.println("Network-down: "+e.getMessage());
          networkUp = false;
        }       
      };
      
    }.start();
  }
    
  private void startCacheSyncThread()
  {
    System.out.println("[INFO] Starting cache-sync thread");
    
    new Thread()
    {
      public void run()
      {
        while (true)
        {
          synchronizeCache();

          try
          {
            sleep(1000);
          }
          catch (InterruptedException e) { }
        }
      };

      public void synchronizeCache()
      {
        if (!USE_IMG_CACHE)
          return;

        long now = System.currentTimeMillis();

        if (lastCacheWrite == 0)
          lastCacheWrite = now;

        if (now - lastCacheWrite > CACHE_WRITE_MS)
        {

          // only write if we have new entries
          if (ImageSearch.cacheSize() != lastCacheSize)
          {

            lastCacheSize = ImageSearch.serializeCache();
            System.out.println("[CACHE] Writing-cache: " + lastCacheSize + " entries");
          }

          lastCacheWrite = now;
        }
      }
    }.start();
  }
 
  public boolean write(String phrase)
  {
    String query = phrase.toLowerCase().replaceAll("(%2C)+", "+");
    
    String status = networkUp ? "ok" : "network-down";
    String[] imgUrls = null;

    if (phrase.endsWith(" -redacted")) {
      
      status = "redacted";
      phrase = phrase.replace(" -redacted", "");
    }
    else if (networkUp) {
      
      try
      {
        imgUrls = imgSearch.fetchThumbnails(query+"+china", 20, true);
        //System.out.println("Found "+imgUrls.length+" thumbnails");
      }
      catch (Throwable e)
      {
        status = e.getMessage();
        warn(status+"\n"+RiTa.stackToString(e));
      }
    }
    
    String json = convertToJSON(status, phrase, imgUrls);

    return writeFile(file, json);
  } 
  
  private static String convertToJSON(String status, String phrase, String[] imgTags)
  {

    ImageResult image = new ImageResult();
    image.query = phrase.replaceAll("\\+", " ");
    image.status = status;
    
    for (int i = 0; imgTags != null && i < imgTags.length; i++)
      image.addImageTag(imgTags[i]);
    
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();
    builder.disableHtmlEscaping();
    
    return builder.create().toJson(image);
  }
  
  public static void log(String msg)
  {
    System.out.println(INFO+new Date()+DELIM+msg);
  }
  
  public String[] fetchThumbnails(String query, int maxResults, boolean randomizeOrder)
  {
    String method = "fetchThumbnails", result[] = null;
    String url = QUERY_TEMPLATE.replaceAll("%QUERY%", query);

    if (useCache) 
      result = (String[]) cacheFetch(method, url);
    
    if (result == null) { // do the fetch
      
      ArrayList<String> list = new ArrayList<String>();
      
      try
      {
        if (PRINT_NETWORK_INFO) System.out.println("Http-get: "+cacheKey(method, url));
        
        Document doc = fetchDocument(url);
  
        if (doc != null) {
          
          Elements divs = doc.select(".rg_i");
          
          for (Element e : divs)
          {
            String src = e.attr("src");
            if (src == null || src.length() < 1)
              src = e.attr("data-src");
            
            list.add(src);//.replaceAll(".*?[&?]imgurl=([^&]+)&.*", "$1"));
          }
        }
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
      
      if (list.size() < 1)  { // no exception, no result
      
        System.err.println("[WARN] No results for '"+query+"'");
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
  public static void warn(String msg) 
  {
    System.err.println(WARN+new Date()+DELIM+msg);
  }
}
