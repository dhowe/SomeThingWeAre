package stwa.server;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import rita.RiTa;
import stwa.ImageResult;
import stwa.ImageSearch;

import com.google.gson.GsonBuilder;

public class JsonHandler extends PhraseHandler
{
	public static boolean USE_IMG_CACHE = false;
	public static int CACHE_WRITE_MS = 5 * 60 * 1000; // 5 min
  
  protected boolean networkUp = true;
  protected ImageSearch imgSearch; 
  protected long lastCacheWrite;
  private int lastCacheSize;
    
  public JsonHandler(String outputFile)
  {
    super(outputFile);
    
    imgSearch = new ImageSearch(USE_IMG_CACHE);
    
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
          if (!networkUp) log("Network-up");
          networkUp = true;
        }
        catch (Throwable e)
        {
          warn("Network-down: "+e.getMessage());
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
      }

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

    if (networkUp) {
      
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

//System.out.println(json);

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
}
