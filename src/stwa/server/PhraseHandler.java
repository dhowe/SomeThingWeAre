package stwa.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public abstract class PhraseHandler implements RfuConstants
{
  protected String file;

  public PhraseHandler(String outputFile) {

    this.file = outputFile;
  }
  
  public abstract boolean write(String phrase);
  
  public void synchronizeCache() {} // no-op 
    
  public boolean writeFile(String afile, String json) 
  {
    try
    {
      FileWriter fw = new FileWriter(afile); 
      fw.write(json);
      fw.flush();
      fw.close();
System.out.println("Wrote: "+System.getProperty("user.dir")+"/"+file+" exists? "+(new File(afile)).exists());
      return true;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  } 

  public String prepareQuery(String phrase)
  {
    return phrase.toLowerCase().replaceAll("(%2C)+", "+");
  }
  
  public static void log(String msg)
  {
    System.out.println(INFO+new Date()+DELIM+msg);
  }
  
  public static void warn(String msg) 
  {
    System.err.println(WARN+new Date()+DELIM+msg);
  }

}
