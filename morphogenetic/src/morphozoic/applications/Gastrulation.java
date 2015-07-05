// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Morphogen;
import morphozoic.Organism;

// Gastrulation.
public class Gastrulation extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.Gastrulation";

   // Options.
   public static final String OPTIONS = "\n\t[-genMetamorphs <save file name>]\n\t[-execMetamorphs <load file name>]";

   // Constructor.
   public Gastrulation(String[] args, Integer randomSeed) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Random numbers.
      this.randomSeed = randomSeed;
      randomizer      = new Random(randomSeed);

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-genMetamorphs"))
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
      if ((genFilename != null) && (execFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -genMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            saveParms(writer);
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
            loadParms(reader);
            Metamorph m;
            while ((m = Metamorph.load(reader)) != null)
            {
               metamorphs.add(m);
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


   @Override
   public void update()
   {
      int x, y, x2, y2, s, s2;

      // Initialize update.
      initUpdate();

      // Update cells.
      if ((execFilename == null) || (tick == 0))
      {
         // Force update.
         x = DIMENSIONS.width / 2;
         y = DIMENSIONS.height / 2;
         if (tick < 9)
         {
            s  = (tick * 2) + 1;
            s2 = s / 2;
            for (y2 = 0; y2 < s; y2++)
            {
               for (x2 = 0; x2 < s; x2++)
               {
                  cells[x + x2 - s2][y + y2 - s2].type = randomizer.nextInt(Cell.NUM_TYPES);
               }
            }
            s -= 2;
            s2 = s / 2;
            for (y2 = 0; y2 < s; y2++)
            {
               for (x2 = 0; x2 < s; x2++)
               {
                  cells[x + x2 - s2][y + y2 - s2].type = Cell.EMPTY;
               }
            }
         }
         else if (tick < 20)
         {
            s = x + 4;
            int t = (9 - tick) + 7;
            for (x2 = x - 3; x2 < s; x2++)
            {
               cells[x2][y - t].type = randomizer.nextInt(Cell.NUM_TYPES);
            }
            s = x + 3;
            t++;
            for (x2 = x - 2; x2 < s; x2++)
            {
               cells[x2][y - t].type = Cell.EMPTY;
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
