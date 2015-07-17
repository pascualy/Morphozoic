// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import rdtree.RDclient;
import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Orientation;
import morphozoic.Parameters;

// C elegans morphogenesis.
public class Celegans extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.Celegans";

   // Morph image template file name.
   public static final String DEFAULT_MORPH_IMAGE_TEMPLATE_FILE = "Celegans.jpg";
   public String              morphImageTemplateFile            = DEFAULT_MORPH_IMAGE_TEMPLATE_FILE;

   // Options.
   public static final String OPTIONS = "\n\t[-morphImageTemplateFile <image template file name with extension>\n\t    \"_[0...N]\" will be inserted before extension to load multiple files\n\t    Defaults to: " + DEFAULT_MORPH_IMAGE_TEMPLATE_FILE + "\n\t]\n\t[-genMetamorphs <save file name>]\n\t[-execMetamorphs <load file name>]";

   // Morph sequence.
   public Vector<Cell[][]> morphSequence;
   public int              morphSequenceIndex;

   // Constructor.
   public Celegans(String[] args, Integer id) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Get arguments.
      boolean morphFile = false;
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-morphImageTemplateFile"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            morphImageTemplateFile = args[i];
            morphFile = true;
         }
         else if (args[i].equals("-genMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            genFilename = args[i];
         }
         else if (args[i].equals("-execMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            execFilename = args[i];
         }
         else
         {
            System.err.println(usage);
            throw new IllegalArgumentException(usage);
         }
      }
      if (morphFile && (execFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -morphImageTemplateFile and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      if ((genFilename != null) && (execFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -genMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      // Load morph sequence.
      if (execFilename == null)
      {
         loadMorphSequences();
         if (morphSequenceIndex >= 0)
         {
            Cell[][] morphCells = (Cell[][])morphSequence.get(morphSequenceIndex);
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  cells[x][y] = morphCells[x][y];
               }
            }
            if (morphSequenceIndex < morphSequence.size() - 1)
            {
               morphSequenceIndex++;
            }
         }
      }

      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            Parameters.saveParms(writer);
         }
         catch (Exception e)
         {
            System.err.println("Cannot save file " + genFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot open save file " + genFilename +
                                  ":" + e.getMessage());
         }
      }
      if (execFilename != null)
      {
         try
         {
            reader = new DataInputStream(new FileInputStream(execFilename));
            Parameters.loadParms(reader);
            int     x, y;
            boolean eof = false;
            for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  cells[x][y].type = Cell.EMPTY;
               }
            }
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  cells[x][y].type = reader.readInt();
                  x = reader.readInt();
               }
            }
            catch (EOFException e)
            {
               eof = true;
            }
            if (!eof)
            {
               Metamorph m;
               if (Parameters.EXEC_METAMORPHS_WITH_SEARCH_TREE)
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                     metamorphSearch.insert((RDclient)m);
                  }
               }
               else
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
               }
            }
         }
         catch (Exception e)
         {
            System.err.println("Cannot load file " + execFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot load file " + execFilename +
                                  ":" + e.getMessage());
         }
         reader.close();
         isEditable = true;
      }
   }


   // Load morph sequences from image files.
   private void loadMorphSequences() throws IllegalArgumentException, IOException
   {
      Vector<Image> images = new Vector<Image>();

      String basename = null;
      String ext      = null;
      String[] parts = morphImageTemplateFile.split("\\.(?=[^\\.]+$)");
      switch (parts.length)
      {
      case 1:
         basename = parts[0];
         break;

      case 2:
         basename = parts[0];
         ext      = parts[1];
         break;

      default:
         throw new IllegalArgumentException("Invalid morphImageFiles option");
      }

      // Load images.
      boolean done  = false;
      Image   image = null;
      for (int i = 0; !done; i++)
      {
         done = true;
         String imageName = basename + "_" + i;
         if (ext != null)
         {
            imageName += "." + ext;
         }

         // Load image as resource.
         try {
            image = (Image)ImageIO.read(getClass().getResource(imageName));
            if (image != null)
            {
               images.add(image);
               done = false;
               continue;
            }
         }
         catch (Exception e)
         {
         }

         // Load external image file.
         try
         {
            image = (Image)ImageIO.read(new File(imageName));
            if (image != null)
            {
               images.add(image);
               done = false;
            }
         }
         catch (Exception e)
         {
         }
      }

      // Create cell sequence from image sequence.
      morphSequence = new Vector<Cell[][]>();
      int   w = Parameters.ORGANISM_DIMENSIONS.width;
      int   h = Parameters.ORGANISM_DIMENSIONS.height;
      float q = 256.0f / (float)Parameters.NUM_CELL_TYPES;
      for (Image i : images)
      {
         Image         s = i.getScaledInstance(w, h, Image.SCALE_DEFAULT);
         BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
         Graphics      g = b.createGraphics();
         g.drawImage(s, 0, 0, null);
         g.dispose();
         Cell[][] c = new Cell[w][h];
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
               c[x][cy] = new Cell(t, x, cy, Orientation.NORTH, this);
            }
         }
         morphSequence.add(c);
      }
      if (morphSequence.size() > 0)
      {
         morphSequenceIndex = 0;
      }
      else
      {
         morphSequenceIndex = -1;
      }
   }


   @Override
   public void update()
   {
      int x, y;

      // Initialize update.
      initUpdate();

      // Save initial cell types?
      if ((tick == 0) && (genFilename != null))
      {
         try
         {
            for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  if (predecessorCells[x][y].type != Cell.EMPTY)
                  {
                     writer.writeInt(x);
                     writer.writeInt(y);
                     writer.writeInt(predecessorCells[x][y].type);
                  }
               }
            }
            writer.writeInt(-1);
            writer.flush();
         }
         catch (IOException e)
         {
            System.err.println("Cannot save save cell types to " + genFilename + ":" + e.getMessage());
         }
      }

      // Update cells.
      if (execFilename == null)
      {
         if (morphSequenceIndex >= 0)
         {
            Cell[][] morphCells = (Cell[][])morphSequence.get(morphSequenceIndex);
            for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  cells[x][y] = morphCells[x][y];
               }
            }
            if (morphSequenceIndex < morphSequence.size() - 1)
            {
               morphSequenceIndex++;
            }
         }
      }
      else
      {
         // Execute metamorphs.
         execMetamorphs();
      }
      tick++;

      // Generate metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         saveMetamorphs();
      }
   }
}
