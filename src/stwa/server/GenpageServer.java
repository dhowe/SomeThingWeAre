package stwa.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.util.Date;

import rita.RiTaException;

public class GenpageServer extends Thread implements RfuConstants
{   
  public static String PAGE_NAME = "genpage",
    IMAGE_SELECTOR = "#rcnt",
    OUTPUT_DIR = ".";
 
  // ----------------------------------------------------------------
    
  private byte[] receiveData;
  private DatagramSocket serverSocket;
	private JsonHandler phraseHandler;
  
  public GenpageServer(String outputFolder, int port) throws SocketException
  {
    setOutputDir(outputFolder);
          
    this.serverSocket = new DatagramSocket(port);
    
    this.phraseHandler = new JsonHandler(OUTPUT_DIR + PAGE_NAME + ".js");

    log("Started " + getClass().getName() + " on port " + port);
  }
  
  public void run()
  {
    while (true)
    {
      receiveData = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      try
      {
        serverSocket.receive(receivePacket);
        String rcvd = new String(receivePacket.getData());
        String ip = receivePacket.getAddress().toString().replaceAll("/", "");
        parseRequest(ip, URLDecoder.decode(rcvd, UTF_8));
        //appMonitor.notifyOnUpdate();
        phraseHandler.synchronizeCache();
      }
      catch (IOException e)
      {
        System.err.println("[ERROR] On receive packet..."+e.getMessage());
      }
    }
  }

  public boolean parseRequest(String ip, String data) {

    String[] params = data.split(AMP);
    //System.out.println(RiTa.asList(params));
    
    String cmd = getString(params, CMD);
    String phrase = getString(params, PHRASE);
    
    if (cmd.equals(GEN_PAGE)) {
      
      log("'"+phrase+"'");
      
      return phraseHandler.write(phrase);
    }
    
    System.err.println("[WARN] Unexpected cmd: "+cmd);
    
    return false;
  }

  public static void setOutputDir(String outputFolder)
  {
    OUTPUT_DIR = outputFolder;
    
    if (!OUTPUT_DIR.endsWith("/")) OUTPUT_DIR += "/";
    
    if (!new File(OUTPUT_DIR).exists()) 
      throw new RuntimeException("No accessible output directory: '"+OUTPUT_DIR+"'");
  }
  
  static void log(String msg)
  {
    System.out.println("[INFO] "+new Date()+"\t"+msg);
  }
  
  static void warn(String msg) 
  {
    System.err.println("[WARN] "+new Date()+"\t"+msg);
  }

  static String getString(String[] params, String key)
  {
    for (int i = 0; i < params.length; i++)
    {
      if (params[i].startsWith(key + EQUALS)) {
        String data = params[i].substring(params[i].indexOf(EQUALS) + 1);
        return data.trim();
      }
    }
    return null;
  }

  static int getInt(String[] params, String key)
  {
    String val = getString(params, key);
    if (val == null || val.length() < 1)
      throw new RiTaException("Bad int param for: " + key);
    return Integer.parseInt(val);
  }
  
  // -----------------------------------------------------
  
  public static void main(String[] args) throws SocketException
  {
//    if (1==0) {
//      SomeServer.setOutputDir("/Users/dhowe/Documents/eclipse-workspace/ReadForUsV3/www/");
//      SomeServer.parseRequest("127.0.0.1", "cmd=genPage&phrase=THE+REAL+STORY+OF&type=json");
//      return;
//    }
//   
    GenpageServer someServer = new GenpageServer("www/", 8010);
		someServer.start();
		someServer.parseRequest("127.0.0.1", "cmd=genPage&phrase=THE+REAL+STORY+OF&type=json");
  }
  
}// end