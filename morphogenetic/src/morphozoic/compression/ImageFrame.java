package morphozoic.compression;

import java.awt.Image;

import javax.swing.JFrame;

class ImageFrame extends JFrame
{
   private static final long serialVersionUID = 0L;

   public void displayImageInFrame(Image img, String title)
   {
      ImagePanel panel = new ImagePanel(img);

      add(panel);
      setVisible(true);
      setSize(panel.getSize());
      setPreferredSize(panel.getSize());
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setTitle(title);
   }
}
