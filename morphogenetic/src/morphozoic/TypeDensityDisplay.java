// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Random;

import javax.swing.*;

public class TypeDensityDisplay extends JFrame implements Runnable, WindowListener
{
   // Morphogenetic field sphere sector.
   Morphogen.Sphere.Sector sector;

   // Display rate (milliseconds).
   static final int DISPLAY_UPDATE_DELAY = 50;

   // Display.
   static final Dimension displaySize = new Dimension(350, 100);
   Canvas                 canvas;
   Graphics               canvasGraphics;
   Dimension              canvasSize;
   Image     image;
   Graphics  imageGraphics;
   Dimension imageSize;
   Thread    displayThread = null;

   // Constructor.
   public TypeDensityDisplay(String title, Morphogen.Sphere.Sector sector, Random randomizer) throws Exception
   {
      // Save sector.
      this.sector = sector;

      // Create display.
      setTitle(title);
      addWindowListener(this);
      canvas     = new Canvas();
      canvasSize = new Dimension(displaySize.width, displaySize.height);
      canvas.setBounds(0, 0, canvasSize.width, canvasSize.height);
      getContentPane().add(canvas);

      // Show.
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int       x          = screenSize.width / 4;
      int       y          = screenSize.height / 4;
      x = randomizer.nextInt(2 * x) + x;
      y = randomizer.nextInt(2 * y) + y;
      this.setLocation(x - this.getSize().width / 2, y - this.getSize().height / 2);
      pack();
      setVisible(true);

      // Get canvas image.
      canvasGraphics = canvas.getGraphics();
      image          = createImage(canvasSize.width, canvasSize.height);
      imageGraphics  = image.getGraphics();
      imageSize      = canvasSize;

      // Create display thread.
      if (displayThread == null)
      {
         displayThread = new Thread(this);
         displayThread.setPriority(Thread.MIN_PRIORITY);
         displayThread.start();
      }
   }


   // Run.
   public void run()
   {
      // Display update loop.
      while (Thread.currentThread() == displayThread &&
             !displayThread.isInterrupted())
      {
         updateDisplay();

         try {
            Thread.sleep(DISPLAY_UPDATE_DELAY);
         }
         catch (InterruptedException e) {
            break;
         }
      }
   }


   // Update display.
   public void updateDisplay()
   {
      // Clear.
      imageGraphics.setColor(Color.white);
      imageGraphics.fillRect(0, 0, imageSize.width, imageSize.height);

      int w = imageSize.width / Cell.NUM_TYPES;
      for (int i = 0, x = 0; i < Cell.NUM_TYPES; i++, x += w)
      {
         imageGraphics.setColor(Cell.getColor(i));
         float h = (float)imageSize.height * sector.getTypeDensity(i);
         imageGraphics.fillRect(x, (int)(imageSize.height - h), w + 1, (int)h);
      }

      imageGraphics.setColor(Color.black);
      for (int i = 0, j = Cell.NUM_TYPES - 1, x = w; i < j; i++, x += w)
      {
         imageGraphics.drawLine(x, 0, x, imageSize.height);
      }

      // Copy to display.
      canvasGraphics.drawImage(image, 0, 0, this);
   }


   public void windowClosing(WindowEvent e)
   {
      if (displayThread != null)
      {
         displayThread.interrupt();
      }
   }


   public void windowClosed(WindowEvent e) {}

   public void windowOpened(WindowEvent e) {}

   public void windowIconified(WindowEvent e) {}

   public void windowDeiconified(WindowEvent e) {}

   public void windowActivated(WindowEvent e) {}

   public void windowDeactivated(WindowEvent e) {}
}
