// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import rdtree.RDclient;
import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Orientation;
import morphozoic.Parameters;

// Image repair demonstration.
public class ImageRepair extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.ImageRepair";

   // Image file names.
   public static final String DEFAULT_TARGET_IMAGE_FILE_NAME = "lena.jpg";
   public static String       TARGET_IMAGE_FILE_NAME         = DEFAULT_TARGET_IMAGE_FILE_NAME;
   public static String       SOURCE_IMAGE_FILE_NAME         = null;
   public static int          TRAIN_MORPHS = 1;

   // Image holes to repair.
   public static final int DEFAULT_NUM_HOLES     = 1;
   public static int       NUM_HOLES             = DEFAULT_NUM_HOLES;
   public static final int DEFAULT_MAX_HOLE_SIZE = 1;
   public static int       MAX_HOLE_SIZE         = DEFAULT_MAX_HOLE_SIZE;

   // Zero cell type is empty?
   public boolean ZERO_CELL_IS_EMPTY = false;

   // Options.
   public static final String OPTIONS =
      "\n\t[-targetImageFilename <image file name> (default: " + DEFAULT_TARGET_IMAGE_FILE_NAME + ")]"
      + "\n\t\t[-sourceImageFilename <image file name>"
      + "\n\t\t\t[-trainMorphs <steps> (default=1)]]"
      + "\n\t|"
      + "\n\t\t[-numHoles <number of holes made in image>]"
      + "\n\t\t[-maxHoleSize <maximum hold size in cells>]"
      + "\n\t[-zeroCellIsEmpty]";

   // Constructor.
   public ImageRepair(String[] args, Integer id) throws Exception
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Get arguments.
      boolean gotHoles       = false;
      boolean gotTrainMorphs = false;
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-targetImageFilename"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            TARGET_IMAGE_FILE_NAME = args[i];
         }
         else if (args[i].equals("-sourceImageFilename"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            SOURCE_IMAGE_FILE_NAME = args[i];
         }
         else if (args[i].equals("-trainMorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            TRAIN_MORPHS = Integer.parseInt(args[i]);
            if (TRAIN_MORPHS < 0)
            {
               System.err.println("Invalid morph steps");
               throw new IllegalArgumentException("Invalid morph steps");
            }
            gotTrainMorphs = true;
         }
         else if (args[i].equals("-numHoles"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            NUM_HOLES = Integer.parseInt(args[i]);
            if (NUM_HOLES < 0)
            {
               System.err.println("Invalid number of holes");
               throw new IllegalArgumentException("Invalid number of holes");
            }
            gotHoles = true;
         }
         else if (args[i].equals("-maxHoleSize"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            MAX_HOLE_SIZE = Integer.parseInt(args[i]);
            if (MAX_HOLE_SIZE < 1)
            {
               System.err.println("Maximum hole size must be positive");
               throw new IllegalArgumentException("Maximum hole size must be positive");
            }
            gotHoles = true;
         }
         else if (args[i].equals("-zeroCellIsEmpty"))
         {
            ZERO_CELL_IS_EMPTY = true;
         }
         else
         {
            System.err.println(usage);
            throw new IllegalArgumentException(usage);
         }
      }
      if (gotHoles && ((SOURCE_IMAGE_FILE_NAME != null) || gotTrainMorphs))
      {
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      if (gotTrainMorphs && (SOURCE_IMAGE_FILE_NAME == null))
      {
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      // Load target image.
      Cell[][] targetCells = new Cell[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      loadImage(TARGET_IMAGE_FILE_NAME);
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            targetCells[x][y] = cells[x][y].clone();
         }
      }

      // Load source image?
      Cell[][] sourceCells = new Cell[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      if (SOURCE_IMAGE_FILE_NAME != null)
      {
         loadImage(SOURCE_IMAGE_FILE_NAME);
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               sourceCells[x][y] = cells[x][y].clone();
            }
         }
      }
      else
      {
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               sourceCells[x][y] = targetCells[x][y].clone();
            }
         }
      }

      // Generate metamorphs.
      Cell[][] updateCells = new Cell[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            updateCells[x][y] = sourceCells[x][y].clone();
         }
      }
      for (int i = 0; i < TRAIN_MORPHS; i++)
      {
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               if ((cells[x][y].type != Cell.EMPTY) && morphogeneticCell(x, y))
               {
                  cells[x][y].generateMorphogen();
               }
               else
               {
                  cells[x][y].morphogen = null;
               }
            }
         }
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               cells[x][y].type = targetCells[x][y].type;
            }
         }
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               if (cells[x][y].morphogen != null)
               {
                  Metamorph metamorph = new Metamorph(cells[x][y].morphogen, cells[x][y]);
                  for (Metamorph m : metamorphs)
                  {
                     if (m.equals(metamorph))
                     {
                        metamorph = null;
                        break;
                     }
                  }
                  if (metamorph != null)
                  {
                     metamorphs.add(metamorph);
                  }
               }
            }
         }
         switch (Parameters.METAMORPH_EXEC_TYPE)
         {
         case LINEAR_SEARCH:
            break;

         case SEARCH_TREE:
            for (Metamorph m : metamorphs)
            {
               metamorphSearch.insert((RDclient)m);
            }
            break;

         case NEURAL_NETWORK:
            createMetamorphNNs();
            break;
         }
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               cells[x][y].type = updateCells[x][y].type;
            }
         }

         // Initialize update.
         initUpdate();

         // Execute metamorphs.
         execMetamorphs();

         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               updateCells[x][y].type = cells[x][y].type;
            }
         }
      }
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y].type = sourceCells[x][y].type;
         }
      }

      // Damage image with holes.
      if (SOURCE_IMAGE_FILE_NAME == null)
      {
         for (int i = 0; i < NUM_HOLES; i++)
         {
            int d  = randomizer.nextInt(MAX_HOLE_SIZE) + 1;
            int o  = d / 2;
            int cx = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.width);
            int cy = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.height);
            for (int x = 0; x < d; x++)
            {
               for (int y = 0; y < d; y++)
               {
                  int   x2 = cx - o + x;
                  int   y2 = cy - o + y;
                  float dx = Math.abs(cx - x2);
                  float dy = Math.abs(cy - y2);
                  if (Math.sqrt((dx * dx) + (dy * dy)) <= o)
                  {
                     cells[Organism.wrapX(x2)][Organism.wrapX(y2)].type = 0;
                  }
               }
            }
         }
      }
   }


   // Load image from image file.
   public void loadImage(String filename) throws IllegalArgumentException, IOException
   {
      Image image = null;

      // Load image as resource.
      try {
         image = (Image)ImageIO.read(getClass().getResource(filename));
      }
      catch (Exception e)
      {
      }

      // Load external image file.
      if (image == null)
      {
         try
         {
            image = (Image)ImageIO.read(new File(filename));
         }
         catch (Exception e)
         {
            System.err.println("Cannot load image " + filename);
            throw new IllegalArgumentException("Cannot load image " + filename);
         }
      }

      // Create image cells.
      int           w = Parameters.ORGANISM_DIMENSIONS.width;
      int           h = Parameters.ORGANISM_DIMENSIONS.height;
      float         q = 256.0f / (float)Parameters.NUM_CELL_TYPES;
      Image         i = image.getScaledInstance(w, h, Image.SCALE_DEFAULT);
      BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      Graphics      g = b.createGraphics();
      g.drawImage(i, 0, 0, null);
      g.dispose();
      for (int x = 0; x < w; x++)
      {
         for (int y = 0; y < h; y++)
         {
            int cy = (h - 1) - y;
            int t  = (int)((float)(b.getRGB(x, y) & 0xFF) / q);
            if (t >= Parameters.NUM_CELL_TYPES)
            {
               t = Parameters.NUM_CELL_TYPES - 1;
            }
            if (ZERO_CELL_IS_EMPTY && (t == 0))
            {
               t = Cell.EMPTY;
            }
            cells[x][cy] = new Cell(t, x, cy, Orientation.NORTH, this);
         }
      }
   }


   @Override
   public void update()
   {
      // Initialize update.
      initUpdate();

      // Execute metamorphs.
      execMetamorphs();

      tick++;
   }
}
