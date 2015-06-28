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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

public class Morphozoic extends JFrame implements Runnable
{
   // Organism.
   String   organismName;
   Organism organism;

   // Update rate (milliseconds).
   public static final int MIN_UPDATE_DELAY = 100;
   public static final int MAX_UPDATE_DELAY = 1000;

   // Display rate (milliseconds).
   public static final int DISPLAY_UPDATE_DELAY = 50;

   // Display.
   public static final Dimension displaySize = new Dimension(500, 500);
   Canvas    canvas;
   Graphics  canvasGraphics;
   Dimension canvasSize;
   double    cellWidth;
   double    cellHeight;
   Image     image;
   Graphics  imageGraphics;
   Dimension imageSize;
   String    statusMessage       = "";
   boolean   drawGrid            = true;
   boolean   displayField        = false;
   boolean   displayFieldProlong = false;
   Cell      displayFieldCell    = null;
   int       displayFieldSphere  = -1;

   // Threads.
   Thread updateThread;
   Thread displayThread;

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
   public static final String OPTIONS = "\n\t[-numCellTypes <number of cell types>]\n\t[-numSpheres <number of morphogenetic spheres>]\n\t[-sectorDimension <sphere sector dimension>]\n\t[-randomSeed <random seed>]";

   // Constructor.
   public Morphozoic(String organismName, String[] organismArgs,
                     Integer randomSeed) throws Exception
   {
      // Create the organism.
      this.organismName = organismName;
      try
      {
         Class<?>       cl   = Class.forName(organismName);
         Constructor<?> cons = cl.getConstructor(String[].class, Integer.class );
         organism = (Organism)cons.newInstance(organismArgs, randomSeed);
      }
      catch (Exception e)
      {
         throw new Exception("Cannot create organism " + organismName);
      }

      // Create display.
      setTitle(organismName);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLayout(new BorderLayout());
      canvas     = new Canvas();
      canvasSize = new Dimension(displaySize.width,
                                 (int)((double)displaySize.height * .95));
      cellWidth  = (double)canvasSize.width / (double)organism.DIMENSIONS.width;
      cellHeight = (double)canvasSize.height / (double)organism.DIMENSIONS.height;
      canvas.setBounds(0, 0, canvasSize.width, canvasSize.height);
      canvas.addMouseListener(new CanvasMouseListener());
      canvas.addKeyListener(new CanvasKeyboardListener());
      canvas.setFocusable(true);
      getContentPane().add(canvas, BorderLayout.NORTH);

      // Create control panel.
      controlPanel     = new Panel();
      controlPanelSize = new Dimension(displaySize.width,
                                       (int)((double)displaySize.height * .05));
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

      // Show app.
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


   // Run.
   public void run()
   {
      // Update loop.
      while (Thread.currentThread() == updateThread &&
             !updateThread.isInterrupted())
      {
         if ((updateDelay < MAX_UPDATE_DELAY) || stepButton.getState())
         {
            synchronized (this)
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
         synchronized (this)
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
      int x, y, x2, y2, x3, y3, w, h;

      // Clear.
      imageGraphics.setColor(Color.white);
      imageGraphics.fillRect(0, 0, imageSize.width, imageSize.height);

      // Draw organism.
      if (organism != null)
      {
         w = organism.DIMENSIONS.width;
         h = organism.DIMENSIONS.height;
         if (displayField)
         {
            Morphogen.Sphere sphere = displayFieldCell.morphogen.getSphere(displayFieldSphere);
            int              n      = Morphogen.SECTOR_DIMENSION * Morphogen.SECTOR_DIMENSION;
            for (int i = 0; i < n; i++)
            {
               Morphogen.Sphere.Sector sector = sphere.getSector(i);
               for (y2 = 0; y2 < sector.d; y2++)
               {
                  for (x2 = 0; x2 < sector.d; x2++)
                  {
                     x3 = x2 + sector.dx + displayFieldCell.x;
                     while (x3 < 0) { x3 += w; }
                     while (x3 >= w) { x3 -= w; }
                     y3 = y2 + sector.dy + displayFieldCell.y;
                     while (y3 < 0) { y3 += h; }
                     while (y3 >= h) { y3 -= h; }
                     imageGraphics.setColor(organism.cells[x3][y3].getColor());
                     imageGraphics.fillRect((int)(cellWidth * (double)x3) - 1,
                                            (int)(cellHeight * (double)(h - (y3 + 1))) - 1,
                                            (int)cellWidth + 1, (int)cellHeight + 1);
                  }
               }
            }
            imageGraphics.setColor(Color.red);
            for (int i = 0; i < n; i++)
            {
               Morphogen.Sphere.Sector sector = sphere.getSector(i);
               x3 = sector.dx + displayFieldCell.x;
               while (x3 < 0) { x3 += w; }
               while (x3 >= w) { x3 -= w; }
               y3 = displayFieldCell.y - sector.dy;
               while (y3 < 0) { y3 += h; }
               while (y3 >= h) { y3 -= h; }
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
               for (x = 1, x2 = (int)cellWidth - 1; x < organism.DIMENSIONS.width;
                    x++, x2 = (int)(cellWidth * (double)x) - 1)
               {
                  imageGraphics.drawLine(x2, 0, x2, y2);
               }
               x2 = imageSize.width;
               for (y = 1, y2 = (int)cellHeight - 1; y < organism.DIMENSIONS.height;
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
         int y  = organism.DIMENSIONS.height - (int)((double)my / cellHeight) - 1;

         if ((x >= 0) && (x < displaySize.width) && (y >= 0) && (y < displaySize.height))
         {
            if (displayField)
            {
               int              w            = organism.DIMENSIONS.width;
               int              h            = organism.DIMENSIONS.height;
               Morphogen.Sphere sphere       = displayFieldCell.morphogen.getSphere(displayFieldSphere);
               boolean          selectSector = false;
               int              n            = Morphogen.SECTOR_DIMENSION * Morphogen.SECTOR_DIMENSION;
               for (int i = 0; i < n; i++)
               {
                  Morphogen.Sphere.Sector sector = sphere.getSector(i);
                  int xmin = sector.dx + displayFieldCell.x;
                  while (xmin < 0) { xmin += w; }
                  while (xmin >= w) { xmin -= w; }
                  int ymin = sector.dy + displayFieldCell.y;
                  while (ymin < 0) { ymin += h; }
                  while (ymin >= h) { ymin -= h; }
                  int xmax = xmin + sector.d - 1;
                  int ymax = ymin + sector.d - 1;
                  if ((x >= xmin) && (x <= xmax) && (y >= ymin) && (y <= ymax))
                  {
                     selectSector = true;
                     try
                     {
                        displayFieldProlong = true;
                        new TypeDensityDisplay("Type densities: sphere=" + displayFieldSphere + " sector=" + i,
                                               sector, organism.randomizer);
                     }
                     catch (Exception ex)
                     {
                        System.err.println("Cannot create type density display");
                     }
                     break;
                  }
               }
               if (!selectSector)
               {
                  if (displayFieldProlong)
                  {
                     displayFieldProlong = false;
                  }
                  else
                  {
                     displayField = false;
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
                     displayFieldSphere = 0;
                     displayField       = true;
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
         if (displayField)
         {
            if (e.getKeyChar() == ' ')
            {
               displayFieldSphere = (displayFieldSphere + 1) % Morphogen.NUM_SPHERES;
            }
            else
            {
               displayField = false;
            }
         }
      }
   }

   // Main.
   public static void main(String[] args)
   {
      String usage        = "Usage: java morphozoic.Morphozoic\n\t[-organism morphozoic.applications.<Organism class name>]" + OPTIONS + "\n\t[organism-specific options]";
      String organismName = Organism.DEFAULT_ORGANISM;
      int    randomSeed   = Organism.DEFAULT_RANDOM_SEED;

      // Get arguments.
      Vector<String> argsVector = new Vector<String>();
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-organism"))
         {
            i++;
            organismName = args[i];
         }
         else if (args[i].equals("-numCellTypes"))
         {
            i++;
            Cell.NUM_TYPES = Integer.parseInt(args[i]);
            if (Cell.NUM_TYPES <= 0)
            {
               System.err.println("Number of cell types must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numSpheres"))
         {
            i++;
            Morphogen.NUM_SPHERES = Integer.parseInt(args[i]);
            if (Morphogen.NUM_SPHERES <= 0)
            {
               System.err.println("Number of spheres must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-sectorDimension"))
         {
            i++;
            Morphogen.SECTOR_DIMENSION = Integer.parseInt(args[i]);
            if ((Morphogen.SECTOR_DIMENSION <= 0) || ((Morphogen.SECTOR_DIMENSION % 2) != 1))
            {
               System.err.println("Sector dimension must be positive odd number");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-randomSeed"))
         {
            i++;
            randomSeed = Integer.parseInt(args[i]);
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
         Morphozoic morphozoic = new Morphozoic(organismName, organismArgs, randomSeed);
      }
      catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }
}
