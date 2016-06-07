package stwa;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

public class ImageUtil
{

  public static File saveImage(String url, String outputFileNoExt)
  {
    String[] tokens = url.split("\\.(?=[^\\.]+$)");
    String ext = tokens[1].toLowerCase();
    File out = new File(outputFileNoExt + "." + ext);
    saveImage(url, out, ext);
    return out;
  }

  // public static File saveImage(String url, File outputFile) {
  // String[] tokens = url.split("\\.(?=[^\\.]+$)");
  // return saveImage(url, outputFile, tokens[1].toLowerCase());
  // }

  public static boolean saveImage(String url, File outputFile, String imageType)
  {
    return saveImage(fetchImage(url), outputFile, imageType);
  }

  public Image cropImage(BufferedImage bi, int x, int y, int w, int h)
  {
    return bi.getSubimage(x, y, w, h);
  }

  public Image scaleImage(BufferedImage bi, int width, int height)
  {

    return bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    /*
     * BufferedImage bufferedThumbnail = new BufferedImage(scaledImg.getWidth(null), scaledImg.getHeight(null), BufferedImage.TYPE_INT_RGB); bufferedThumbnail.getGraphics().drawImage(scaledImg, 0, 0, null);
     */
    // return bufferedThumbnail;
  }

  public static boolean saveImage(BufferedImage bi, File outputFile, String imageType)
  {

    try
    {
      if (!ImageIO.write(bi, imageType, outputFile))
        throw new RuntimeException("Unable to write image: " + bi + " " + imageType);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    if (!outputFile.exists())
      throw new RuntimeException("*** File not saved: " + outputFile);

    return true;
  }

  public static int[] getImagePixels(BufferedImage image)
  {
    int[] pixels = new int[image.getWidth() * image.getHeight()];
    try
    {
      new PixelGrabber(image, 0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth()).grabPixels(0);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    return pixels;
  }

  public static PImage fetchPImage(PApplet pApplet, String url)
  {
    return toPImage((BufferedImage) fetchImage(url));
  }

  private static PImage toPImage2(PApplet pApplet, BufferedImage bimg)
  {
    int imgW = bimg.getWidth(pApplet), imgH = bimg.getHeight(pApplet);

    PImage pImage = pApplet.createImage(imgW, imgH, PConstants.RGB);
    pImage.loadPixels();

    for (int row = 0; row < imgH; row++)
    {
      for (int col = 0; col < imgW; col++)
      {
        int idx = row * imgW + col;
        pImage.pixels[idx] = bimg.getRGB(col, row);
      }
    }
    pImage.updatePixels();

    return pImage;
  }

  public static BufferedImage toBufferedImage(PImage img)
  {

    return (BufferedImage) img.getNative();
  }

  public static PImage toPImage(BufferedImage bimg)
  {

    if (bimg == null)
      return null;

    PImage img = new PImage(bimg.getWidth(), bimg.getHeight(), PConstants.ARGB);
    bimg.getRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
    img.updatePixels();

    return img;
  }

  public static BufferedImage fetchImage(String url)
  {
    try
    {
      return ImageIO.read(new URL(url));
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public static BufferedImage fetchImage(File f)
  {
    try
    {
      return ImageIO.read(f);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
  /*
   * public static Image crop(Image img, int w, int h) { int x = 0, y = 0; Toolkit toolkit = Toolkit.getDefaultToolkit(); CropImageFilter cropFilter = new CropImageFilter (x, y, w, h); Image croppedImage = toolkit.createImage(new
   * FilteredImageSource (img.getSource(), cropFilter)); return croppedImage; }
   * 
   * public static Image scale(Image img, float scaleFactor) {
   * 
   * }
   * 
   * public static Image resizeImage(Image img, int w, int h) { int type = BufferedImage.TYPE_INT_ARGB;
   * 
   * BufferedImage resizedImage = new BufferedImage(w, h, type); Graphics2D g = resizedImage.createGraphics();
   * 
   * g.setComposite(AlphaComposite.Src); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
   * g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
   * 
   * g.drawImage(img, 0, 0, w, h, null); g.dispose();
   * 
   * return resizedImage; }
   */

  // untested
  private static byte[] pImageToByteArray(PApplet p, PImage pImg)
  {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BufferedImage img = new BufferedImage(pImg.width, pImg.height, 2);
    img = (BufferedImage) p.createImage(pImg.width, pImg.height);
    pImg.loadPixels();
    for (int i = 0; i < pImg.width; i++)
    {
      for (int j = 0; j < pImg.height; j++)
      {
        int id = j * pImg.width + i;
        img.setRGB(i, j, pImg.pixels[id]);
      }
    }
    try
    {
      // JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
      // encoder.encode(img);
      ImageIO.write(img, "jpeg", out);

    }
    catch (Exception e)
    {
      System.out.println(e);
    }
    return out.toByteArray();
  }

  public static BufferedImage resize(BufferedImage img, int w, int h)
  {

    double target = w / (double) h, actual = img.getWidth() / (double) img.getHeight();
    Scalr.Mode mode = (target > actual) ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
    return Scalr.resize(img, Scalr.Method.SPEED, mode, w, h, Scalr.OP_ANTIALIAS);
  }

  public static BufferedImage resizeExact(BufferedImage img, int w, int h)
  {

    int verticalCropDivisor = 5;

    long ts = System.currentTimeMillis();
    System.out.print("  Crop/resize: " + img.getWidth() + "x" + img.getHeight());

    BufferedImage resized = resize(img, w, h);

    System.out.print(" to " + resized.getWidth() + "x" + resized.getHeight());

    int x = 0, y = 0;
    if (resized.getWidth() > w)
      x = (resized.getWidth() - w) / 2;
    else if (resized.getHeight() > h)
    {
      System.out.print(" VERTICAL");
      y = ((resized.getHeight() - h)) / 2 - ((resized.getHeight() - h) / 10); // TODO
    }

    BufferedImage cropped = Scalr.crop(resized, x, y, w, h);

    System.out.println(" to " + cropped.getWidth() + "x" + cropped.getHeight() + " in " + (System.currentTimeMillis() - ts) + "ms");

    return cropped;
  }

  public static void main(String[] args) throws IOException
  {
    File in = new File("/Users/dhowe/Documents/Workspaces/eclipse-workspace/SomeThingWeAre/src/data/scale-test.jpg");
    File out = new File("/Users/dhowe/Documents/Workspaces/eclipse-workspace/SomeThingWeAre/src/data/scale-result.jpg");
    BufferedImage img = ImageIO.read(in);
    // System.out.print("Resizing "+img.getWidth()+"x"+img.getHeight());
    BufferedImage result = resizeExact(img, 960, 540);

    if (!ImageIO.write(result, "jpg", new FileOutputStream(out)))
      throw new RuntimeException("Write failed");
    // System.out.println(" to "+result.getWidth()+"x"+result.getHeight());
  }
}
