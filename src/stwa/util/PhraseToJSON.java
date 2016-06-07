package stwa.util;

import rita.RiTa;
import stwa.SomeSuper;

public class PhraseToJSON
{
  private static int frameCountAdjustment = 0; // EDIT !

  public static String parsePhraseFile(String timeStampsFileName, String choppedPhrasesFileName)
  {
    String[] lines = RiTa.loadStrings(choppedPhrasesFileName);
    String[] stamps = RiTa.loadStrings(timeStampsFileName);
    if (lines.length != stamps.length)
      System.out.println("[WARN] File lengths are not equal.");
    for (int i = 0; i < lines.length; i++)
    {
      String[] items = stamps[i].split(",");
      String stamp = items[3]; // item 3 is the end of the region
      stamp = "0" + stamp.replaceFirst("(?<=.:..:..):", ";");
      lines[i] += (" % " + stamp);
      // System.out.println(lines[i]); // DEBUG
    }
    return parsePhraseFile(lines);
  }

  public static String parsePhraseFile(String textFileName)
  {
    String[] lines = RiTa.loadStrings(textFileName);
    return parsePhraseFile(lines);
  }

  public static String parsePhraseFile(String[] lines)
  {

    int frameIdx = 0; // keep track of where we are

    String json = "[\n";
    json += "  { \"index\": 0, \"text\": \"TITLE\", \"frames\": 45 },\n";
    String[] phrases = new String[lines.length];
    int[] counts = new int[lines.length];
    int totalFrames = 0, framesToShow = 0, shots = 0;
    for (int i = 0; i < lines.length; i++)
    {
      String[] parts = lines[i].split("%");
      if (parts.length != 2)
        throw new RuntimeException("Bad data: " + lines[i]);
      int frameNum = parseTimeCodeMs(parts[1].trim());
      // the frameNum is an absolute number at the end of a region
      phrases[i] = parts[0].trim();
      counts[i] = frameNum - frameIdx;
      frameIdx += counts[i];
      // use the following only if frame counts in the source
      // are not incremental:
      // counts[i] = frameNum;
      framesToShow = counts[i] + frameCountAdjustment; // HACK !

      json += "  { \"index\": " + (i + 1) + ", \"text\": \"" + phrases[i] + "\", \"frames\": " + framesToShow + " }";
      // adding 1 to "index" in order to allow for manual
      // insertion of a TITLE with its own frame count
      totalFrames += framesToShow;

      json += ",\n"; // i < lines.length - 1 ? ",\n" : "\n";
      shots = i + 2;
    }
    json += "  { \"index\": " + shots + ", \"text\": \"CREDITS\", \"frames\": 100 }";
    return json + "\n]" + "\nTotal frames: " + totalFrames;
  }

  static int parseTimeCodeMs(String tc)
  {
    float fps = SomeSuper.fps, fpm = fps * 60, fph = fpm * 60;
    String regex = "([0-9][0-9]):([0-9][0-9]):([0-9][0-9]);([0-9][0-9])";
    String[] parts = tc.replaceAll(regex, "$1%$2%$3%$4").split("%");

    int partial = Integer.parseInt(parts[3].trim());
    int seconds = Integer.parseInt(parts[2].trim());
    int minutes = Integer.parseInt(parts[1].trim());
    int hours = Integer.parseInt(parts[0].trim());

    return (int) ((hours * fph) + (minutes * fpm) + (seconds * fps) + partial);
  }

  public static void main(String[] args)
  {
    System.out.println(parsePhraseFile("STWA_AllReaperRegions.txt", "STWA_AllChoppedPhrases.txt"));
    // System.out.println(parsePhraseFile("poc/phraseCounts.txt"));
  }
}
