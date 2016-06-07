package stwa;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JSoupUtil
{
  public static final Object NET_MONITOR = new Object();
  
  public static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.56 Safari/536.5";

  public static String socksProxyHost, socksProxyPort;

  // fetches the whole HTML document
  public static String fetchHtml(String url) {
    
    return fetchDocument(url).html();
  }

  // fetches the HTML for the (single) specified selector
  public static String fetchHtml(String url, String selector) {
    
    return fetchElement(url, selector).html();
  }
    
  // fetches the Element for the (single) specified selector
  public static Element fetchElement(String url, String selector) {
    
    Elements eles = fetchElements(url, selector);
    
    if (eles.size() != 1) {
      throw new RuntimeException
        ("Unable to select (single) element with selector="
            +selector+", found"+eles.size()+" elements");
    }
    
    return eles.get(0);
  }
  
  // fetches Elements for the specified selector
  public static Elements fetchElements(String url, String selector) {

    return fetchDocument(url).select(selector);
  }
  
  // fetches Document for the specified url
  public static Document fetchDocument(String url) {

    //System.out.println("JSoupUtil.fetchDocument: "+url);
    
    if (socksProxyHost != null)
      System.setProperty("socksProxyHost", socksProxyHost);

    if (socksProxyPort != null)
      System.setProperty("socksProxyPort", socksProxyPort);
    
    // add cookie, etc. handling here?
    synchronized(JSoupUtil.class) {
           
      try
      {
        return Jsoup.connect(url).timeout(5000).userAgent(USER_AGENT).get();
      }
      catch (Throwable e)
      {
        throw new RuntimeException(e);
      }
    }
  }
  
  public static void main(String[] args)
  {
    String url = "https://www.google.com/search?q=the+real+story+of&hl=en&noj=1&tbs=isz:l,ic:gray,itp:photo&tbm=isch&source=lnt&sa=X";
    //System.out.println(fetchHtml(url);
    //System.out.println(fetchHtml(url, "#rcnt"));
  }
}
