// Edge detection.

package morphozoic.compression;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class EdgeDetection
{
   public static String inputFilename  = "lena.jpg";
   public static String outputFilename = "lena_edges.jpg";

   public void detectEdges()
   {
      // Read the RGB image.
      Mat rgbImage = Imgcodecs.imread(inputFilename);

      // Gray image.
      Mat imageGray = new Mat();

      // Canny image.
      Mat imageCanny = new Mat();

      // Show the RGB Image.
      ImageUtils.displayImage(ImageUtils.toBufferedImage(rgbImage), "RGB image");

      // Convert the image in to gray image.
      Imgproc.cvtColor(rgbImage, imageGray, Imgproc.COLOR_BGR2GRAY);

      // Show the gray image.
      ImageUtils.displayImage(ImageUtils.toBufferedImage(imageGray), "Gray image");

      // Canny edge detection.
      Imgproc.Canny(imageGray, imageCanny, 100, 200, 3, true);

      // Show the Canny edge detection image
      ImageUtils.displayImage(ImageUtils.toBufferedImage(imageCanny), "Canny edge detection image");

      // Save the edge detection image.
      System.out.println(String.format("Writing edge detection image: %s", outputFilename));
      Imgcodecs.imwrite(outputFilename, imageCanny);
   }


   public static void main(String[] args)
   {
      switch (args.length)
      {
      case 0:
         break;

      case 2:
         EdgeDetection.outputFilename = args[1];

      case 1:
         EdgeDetection.inputFilename = args[0];
         break;

      default:
         System.err.println("Options: [input filename [output filename]]");
         return;
      }
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
      EdgeDetection edgeDetection = new EdgeDetection();
      edgeDetection.detectEdges();
   }
}
