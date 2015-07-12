/*
 * Copyright (c) 2015 Tom Portegys (portegys@gmail.com). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY TOM PORTEGYS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package morphozoic;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

public class Morphozoic extends JFrame implements Runnable
{
   private static final long serialVersionUID = 1L;

   // Organism.
   String   organismName;
   Organism organism;

   // Update rate (milliseconds).
   public static final int MIN_UPDATE_DELAY = 100;
   public static final int MAX_UPDATE_DELAY = 1000;

   // Display rate (milliseconds).
   public static final int DISPLAY_UPDATE_DELAY = 50;

   // Display.
   public static final Dimension DEFAULT_DISPLAY_SIZE = new Dimension(500, 550);
   static Dimension              DISPLAY_SIZE         = DEFAULT_DISPLAY_SIZE;
   Canvas    canvas;
   Graphics  canvasGraphics;
   Dimension canvasSize;
   double    cellWidth;
   double    cellHeight;
   Image     image;
   Graphics  imageGraphics;
   Dimension imageSize;
   String    statusMessage            = "";
   boolean   drawGrid                 = true;
   boolean   displayField             = false;
   Cell      displayFieldCell         = null;
   int       displayFieldNeighborhood = -1;
   boolean[]       displaySectorTypeDensity;

   // Threads.
   Thread updateThread;
   Thread displayThread;
   Object lock = new Object();

   // Control panel.
   Panel     controlPanel;
   Dimension controlPanelSize;
   Checkbox  stepButton;
   JSlider   updateSlider;
   int       updateDelay = MAX_UPDATE_DELAY;

   // Font.
   Font        font = new Font("Helvetica", Font.BOLD, 12);
   FontMetrics fontMetrics;
   int         fontAscent;
   int         fontWidth;
   int         fontHeight;

   // Options.
   public static final String OPTIONS = "\n\t[-displaySize <width> <height>]\n\t[-organismDimensions <width> <height> (# cells)]\n\t[-numCellTypes <number of cell types>]\n\t[-neighborhoodDimension <cell neighborhood dimension>]\n\t[-numNeighborhoods <number of nested neighborhoods>]\n\t[-morphogeneticCellDispersion <morphogenetic cell dispersiony>]\n\t[-randomSeed <random seed>]";

   // Constructor.
   public Morphozoic(String organismName, String[] organismArgs) throws Exception
   {
      // Create the organism.
      this.organismName = organismName;
      try
      {
         Class<?>       cl   = Class.forName(organismName);
         Constructor<?> cons = cl.getConstructor(String[].class, Integer.class );
         organism = (Organism)cons.newInstance(organismArgs, 0);
      }
      catch (Exception e)
      {
         throw new Exception("Cannot create organism " + organismName);
      }

      // Initialize sector type density selector.
      int n = Parameters.NEIGHBORHOOD_DIMENSION * Parameters.NEIGHBORHOOD_DIMENSION;
      displaySectorTypeDensity = new boolean[n];
      for (int i = 0; i < n; i++)
      {
         displaySectorTypeDensity[i] = false;
      }

      // Create display.
      setTitle(organismName);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLayout(new BorderLayout());
      createDisplay();

      // Resize listener.
      addComponentListener(new ComponentAdapter()
                           {
                              public void componentResized(ComponentEvent e)
                              {
                                 synchronized (lock)
                                 {
                                    int w = getWidth();
                                    int h = getHeight();
                                    if ((DISPLAY_SIZE.width != w) || (DISPLAY_SIZE.height != h))
                                    {
                                       DISPLAY_SIZE = new Dimension(w, h);
                                       getContentPane().remove(canvas);
                                       canvas = null;
                                       getContentPane().remove(controlPanel);
                                       controlPanel = null;
                                       validate();
                                       createDisplay();
                                       DISPLAY_SIZE.width = getWidth();
                                       DISPLAY_SIZE.height = getHeight();
                                    }
                                 }
                              }
                           }
                           );

      // Create update thread.
      if ((updateThread == null) && (organism != null))
      {
         updateThread = new Thread(this);
         updateThread.setPriority(Thread.MIN_PRIORITY);
         updateThread.start();
      }

      // Create display thread.
      if (displayThread == null)
      {
         displayThread = new Thread(this);
         displayThread.setPriority(Thread.MIN_PRIORITY);
         displayThread.start();
      }
   }


   // Create display..
   private void createDisplay()
   {
      // Create canvas.
      canvas     = new Canvas();
      canvasSize = new Dimension(DISPLAY_SIZE.width,
                                 (int)((double)DISPLAY_SIZE.height * .9));
      cellWidth  = (double)canvasSize.width / (double)Parameters.ORGANISM_DIMENSIONS.width;
      cellHeight = (double)canvasSize.height / (double)Parameters.ORGANISM_DIMENSIONS.height;
      canvas.setBounds(0, 0, canvasSize.width, canvasSize.height);
      canvas.addMouseListener(new CanvasMouseListener());
      canvas.addKeyListener(new CanvasKeyboardListener());
      canvas.setFocusable(true);
      getContentPane().add(canvas, BorderLayout.NORTH);

      // Create control panel.
      controlPanel     = new Panel();
      controlPanelSize = new Dimension(DISPLAY_SIZE.width,
                                       (int)((double)DISPLAY_SIZE.height * .1));
      controlPanel.setBounds(0, canvasSize.height, controlPanelSize.width,
                             controlPanelSize.height);
      getContentPane().add(controlPanel, BorderLayout.SOUTH);
      stepButton = new Checkbox("Step");
      controlPanel.add(stepButton);
      controlPanel.add(new Label("Fast", Label.RIGHT));
      updateSlider = new JSlider(Scrollbar.HORIZONTAL, MIN_UPDATE_DELAY,
                                 MAX_UPDATE_DELAY, MAX_UPDATE_DELAY);
      updateSlider.addChangeListener(new updateSliderListener());
      controlPanel.add(updateSlider);
      controlPanel.add(new Label("Stop", Label.LEFT));

      // Pack and show.
      pack();
      setVisible(true);

      // Get canvas image.
      canvasGraphics = canvas.getGraphics();
      image          = createImage(canvasSize.width, canvasSize.height);
      imageGraphics  = image.getGraphics();
      imageSize      = canvasSize;

      // Set font data.
      Graphics g = getGraphics();
      g.setFont(font);
      fontMetrics = g.getFontMetrics();
      fontAscent  = fontMetrics.getMaxAscent();
      fontWidth   = fontMetrics.getMaxAdvance();
      fontHeight  = fontMetrics.getHeight();
   }


   // Run.
   public void run()
   {
      // Update loop.
      while (Thread.currentThread() == updateThread &&
             !updateThread.isInterrupted())
      {
         if ((updateDelay < MAX_UPDATE_DELAY) || stepButton.getState())
         {
            synchronized (lock)
            {
               organism.update();
            }
         }

         if (stepButton.getState())
         {
            stepButton.setState(false);
            updateDelay = MAX_UPDATE_DELAY;
            updateSlider.setValue(updateDelay);
         }

         // Set the timer for the next loop.
         try {
            Thread.sleep(updateDelay);
         }
         catch (InterruptedException e) {
            break;
         }
      }

      // Display update loop.
      while (Thread.currentThread() == displayThread &&
             !displayThread.isInterrupted())
      {
         synchronized (lock)
         {
            updateDisplay();
         }

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
      int   i, j, x, y, x2, y2, x3, y3, w, h, cw, ch;
      float d;

      // Clear.
      imageGraphics.setColor(Color.white);
      imageGraphics.fillRect(0, 0, imageSize.width, imageSize.height);

      // Draw organism.
      if (organism != null)
      {
         w = Parameters.ORGANISM_DIMENSIONS.width;
         h = Parameters.ORGANISM_DIMENSIONS.height;
         if (displayField)
         {
            Morphogen.Neighborhood neighborhood = displayFieldCell.morphogen.getNeighborhood(displayFieldNeighborhood);
            int n = Parameters.NEIGHBORHOOD_DIMENSION * Parameters.NEIGHBORHOOD_DIMENSION;
            for (i = 0; i < n; i++)
            {
               Morphogen.Neighborhood.Sector sector = neighborhood.getSector(i);
               if (displaySectorTypeDensity[i])
               {
                  x3 = Organism.wrapX(sector.dx + displayFieldCell.x);
                  y3 = Organism.wrapY(sector.dy + displayFieldCell.y);
                  cw = ((int)(cellWidth * (double)sector.d) + 1) / Parameters.NUM_CELL_TYPES;
                  for (j = 0, x = (int)(cellWidth * (double)x3) - 1; j < Parameters.NUM_CELL_TYPES; j++, x += cw)
                  {
                     imageGraphics.setColor(Cell.getColor(j));
                     d  = sector.getTypeDensity(j);
                     ch = (int)(cellHeight * (float)sector.d * d);
                     y  = (int)(cellHeight * (double)(h - (y3 + sector.d)));
                     y += (int)(cellHeight * (float)sector.d) - ch;
                     imageGraphics.fillRect(x, y, cw + 1, ch);
                  }
                  imageGraphics.setColor(Color.green);
                  for (j = 0, x = (int)(cellWidth * (double)x3) + cw - 1; j < Parameters.NUM_CELL_TYPES - 1; j++, x += cw)
                  {
                     ch = (int)(cellHeight * (float)sector.d);
                     y  = (int)(cellHeight * (double)(h - (y3 + sector.d)));
                     imageGraphics.drawLine(x, y, x, y + ch);
                  }
               }
               else
               {
                  for (y2 = 0; y2 < sector.d; y2++)
                  {
                     for (x2 = 0; x2 < sector.d; x2++)
                     {
                        x3 = Organism.wrapX(x2 + sector.dx + displayFieldCell.x);
                        y3 = Organism.wrapY(y2 + sector.dy + displayFieldCell.y);
                        imageGraphics.setColor(organism.cells[x3][y3].getColor());
                        imageGraphics.fillRect((int)(cellWidth * (double)x3) - 1,
                                               (int)(cellHeight * (double)(h - (y3 + 1))) - 1,
                                               (int)cellWidth + 1, (int)cellHeight + 1);
                     }
                  }
               }
            }
            imageGraphics.setColor(Color.red);
            for (i = 0; i < n; i++)
            {
               Morphogen.Neighborhood.Sector sector = neighborhood.getSector(i);
               x3 = Organism.wrapX(sector.dx + displayFieldCell.x);
               y3 = Organism.wrapY(displayFieldCell.y - sector.dy);
               imageGraphics.drawRect((int)(cellWidth * (double)x3) - 1,
                                      (int)(cellHeight * (double)(h - (y3 + 1))) - 1,
                                      (int)(cellWidth * (double)sector.d) + 1, (int)(cellHeight * sector.d) + 1);
            }
         }
         else
         {
            for (x = x2 = 0; x < w; x++, x2 = (int)(cellWidth * (double)x))
            {
               for (y = 0, y2 = imageSize.height - (int)cellHeight;
                    y < h; y++, y2 = (int)(cellHeight * (double)(h - (y + 1))))
               {
                  imageGraphics.setColor(organism.cells[x][y].getColor());
                  imageGraphics.fillRect(x2 - 1, y2 - 1,
                                         (int)cellWidth + 1, (int)cellHeight + 1);
               }
            }
            imageGraphics.setColor(Color.black);
            if (drawGrid)
            {
               y2 = imageSize.height;
               for (x = 1, x2 = (int)cellWidth - 1; x < Parameters.ORGANISM_DIMENSIONS.width;
                    x++, x2 = (int)(cellWidth * (double)x) - 1)
               {
                  imageGraphics.drawLine(x2, 0, x2, y2);
               }
               x2 = imageSize.width;
               for (y = 1, y2 = (int)cellHeight - 1; y < Parameters.ORGANISM_DIMENSIONS.height;
                    y++, y2 = (int)(cellHeight * (double)y) - 1)
               {
                  imageGraphics.drawLine(0, y2, x2, y2);
               }
            }
         }
      }

      // Display message
      if (!statusMessage.equals(""))
      {
         imageGraphics.setFont(font);
         imageGraphics.setColor(Color.black);
         imageGraphics.drawString(statusMessage,
                                  (imageSize.width - fontMetrics.stringWidth(statusMessage)) / 2,
                                  imageSize.height / 2);
      }

      // Copy to display.
      canvasGraphics.drawImage(image, 0, 0, this);
   }


   // Update rate slider listener.
   class updateSliderListener implements ChangeListener
   {
      public void stateChanged(ChangeEvent evt)
      {
         updateDelay = updateSlider.getValue();
      }
   }

   // Canvas mouse listener.
   private class CanvasMouseListener extends MouseAdapter
   {
      // Mouse pressed.
      public void mousePressed(MouseEvent e)
      {
         int mx = e.getX();
         int my = e.getY();
         int x  = (int)((double)mx / cellWidth);
         int y  = Parameters.ORGANISM_DIMENSIONS.height - (int)((double)my / cellHeight) - 1;

         if ((x >= 0) && (x < DISPLAY_SIZE.width) && (y >= 0) && (y < DISPLAY_SIZE.height))
         {
            synchronized (lock)
            {
               if (displayField)
               {
                  Morphogen.Neighborhood neighborhood = displayFieldCell.morphogen.getNeighborhood(displayFieldNeighborhood);
                  boolean                selectSector = false;
                  int n = Parameters.NEIGHBORHOOD_DIMENSION * Parameters.NEIGHBORHOOD_DIMENSION;
                  for (int i = 0; i < n; i++)
                  {
                     Morphogen.Neighborhood.Sector sector = neighborhood.getSector(i);
                     int xmin = Organism.wrapX(sector.dx + displayFieldCell.x);
                     int ymin = Organism.wrapY(sector.dy + displayFieldCell.y);
                     int xmax = xmin + sector.d - 1;
                     int ymax = ymin + sector.d - 1;
                     if ((x >= xmin) && (x <= xmax) && (y >= ymin) && (y <= ymax))
                     {
                        selectSector = true;
                        displaySectorTypeDensity[i] = !displaySectorTypeDensity[i];
                        break;
                     }
                  }
                  if (!selectSector)
                  {
                     displayField = false;
                     for (int i = 0; i < n; i++)
                     {
                        displaySectorTypeDensity[i] = false;
                     }
                  }
               }
               else
               {
                  if (organism.isEditable)
                  {
                     if (organism.cells[x][y].type == Cell.EMPTY)
                     {
                        organism.cells[x][y].type = 0;
                     }
                     else
                     {
                        organism.cells[x][y].type = Cell.EMPTY;
                     }
                  }
                  else
                  {
                     if (organism.cells[x][y].type != Cell.EMPTY)
                     {
                        displayFieldCell = organism.cells[x][y].clone();
                        displayFieldCell.generateMorphogen();
                        displayFieldNeighborhood = 0;
                        displayField             = true;
                     }
                  }
               }
            }
         }
      }
   }

   // Canvas keyboard listener.
   private class CanvasKeyboardListener implements KeyListener
   {
      @Override
      public void keyPressed(KeyEvent e)
      {
      }


      @Override
      public void keyReleased(KeyEvent e)
      {
      }


      @Override
      public void keyTyped(KeyEvent e)
      {
         synchronized (lock)
         {
            if (displayField)
            {
               if (e.getKeyChar() == ' ')
               {
                  displayFieldNeighborhood = (displayFieldNeighborhood + 1) % Parameters.NUM_NEIGHBORHOODS;
               }
               else
               {
                  displayField = false;
               }
               int n = Parameters.NEIGHBORHOOD_DIMENSION * Parameters.NEIGHBORHOOD_DIMENSION;
               for (int i = 0; i < n; i++)
               {
                  displaySectorTypeDensity[i] = false;
               }
            }
         }
      }
   }

   // Main.
   public static void main(String[] args)
   {
      String usage        = "Usage: java morphozoic.Morphozoic\n\t[-organism morphozoic.applications.<Organism class name>]" + OPTIONS + "\n\t[organism-specific options]";
      String organismName = Parameters.DEFAULT_ORGANISM;

      // Get arguments.
      Vector<String> argsVector = new Vector<String>();
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-organism"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            organismName = args[i];
         }
         else if (args[i].equals("-displaySize"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int w = Integer.parseInt(args[i]);
            if (w <= 0)
            {
               System.err.println("Display width must be positive");
               System.err.println(usage);
               return;
            }
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int h = Integer.parseInt(args[i]);
            if (h <= 0)
            {
               System.err.println("Display height must be positive");
               System.err.println(usage);
               return;
            }
            DISPLAY_SIZE = new Dimension(w, h);
         }
         else if (args[i].equals("-organismDimensions"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int w = Integer.parseInt(args[i]);
            if (w <= 0)
            {
               System.err.println("Organism width dimension must be positive");
               System.err.println(usage);
               return;
            }
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int h = Integer.parseInt(args[i]);
            if (h <= 0)
            {
               System.err.println("Organism height dimension must be positive");
               System.err.println(usage);
               return;
            }
            Parameters.ORGANISM_DIMENSIONS = new Dimension(w, h);
         }
         else if (args[i].equals("-numCellTypes"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.NUM_CELL_TYPES = Integer.parseInt(args[i]);
            if (Parameters.NUM_CELL_TYPES <= 0)
            {
               System.err.println("Number of cell types must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-neighborhoodDimension"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.NEIGHBORHOOD_DIMENSION = Integer.parseInt(args[i]);
            if ((Parameters.NEIGHBORHOOD_DIMENSION <= 0) || ((Parameters.NEIGHBORHOOD_DIMENSION % 2) != 1))
            {
               System.err.println("Neighborhood dimension must be positive odd number");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numNeighborhoods"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.NUM_NEIGHBORHOODS = Integer.parseInt(args[i]);
            if (Parameters.NUM_NEIGHBORHOODS <= 0)
            {
               System.err.println("Number of neighborhoods must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-morphogeneticCellDispersion"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.MORPHOGENETIC_DISPERSION_MODULO = Integer.parseInt(args[i]);
            if (Parameters.MORPHOGENETIC_DISPERSION_MODULO < 1)
            {
               System.err.println("Morphogenetic cell dispersion must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.RANDOM_SEED = Integer.parseInt(args[i]);
         }
         else if (args[i].equals("-help"))
         {
            System.out.println(usage);
            return;
         }
         else
         {
            argsVector.add(args[i]);
         }
      }

      String[] organismArgs = new String[argsVector.size()];
      for (int i = 0, j = argsVector.size(); i < j; i++)
      {
         organismArgs[i] = argsVector.get(i);
      }
      try
      {
         new Morphozoic(organismName, organismArgs);
      }
      catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }
}
