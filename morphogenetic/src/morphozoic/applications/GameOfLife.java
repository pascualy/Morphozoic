// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import rdtree.RDclient;
import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Parameters;

// Game of Life.
public class GameOfLife extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.GameOfLife";

   // Options.
   public static final String OPTIONS =
      "\n\t[-genMetamorphs <save file name>]"
      + "\n\t[-execMetamorphs <load file name> (overrides command-line parameters)]";

   // Constructor.
   public GameOfLife(String[] args, Integer id) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-genMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            genFilename = args[i];
         }
         else if (args[i].equals("-execMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
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
         System.err.println("Mutually exclusive options: -genMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      isEditable = true;
      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            Parameters.save(writer);
         }
         catch (Exception e)
         {
            System.err.println("Cannot save file " + genFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot save file " + genFilename +
                                  ":" + e.getMessage());
         }
      }
      if (execFilename != null)
      {
         try
         {
            reader = new DataInputStream(new FileInputStream(execFilename));
            Parameters.load(reader);
            init();
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
                  cells[x][y].type = 0;
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
               switch (Parameters.METAMORPH_EXEC_TYPE)
               {
               case LINEAR_SEARCH:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
                  break;

               case SEARCH_TREE:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                     metamorphSearch.insert((RDclient)m);
                  }
                  break;

               case NEURAL_NETWORK:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
                  createMetamorphNNs();
                  break;
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
      }
   }


   @Override
   public void update()
   {
      // Initialize update.
      initUpdate();

      // Save initial cell types?
      if ((tick == 0) && (genFilename != null))
      {
         try
         {
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  if (predecessorCells[x][y].type != Cell.EMPTY)
                  {
                     writer.writeInt(x);
                     writer.writeInt(y);
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
         // Step Game of Life.
         step();
      }
      else
      {
         // Execute metamorphs.
         execMetamorphs();
      }
      tick++;
      if (genFilename != null)
      {
         isEditable = false;
      }

      // Generate metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         saveMetamorphs();
      }
   }


   // Step Game of Life.
   private void step()
   {
      int x, y, x2, y2, w, h, count;

      // Clear cells.
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y].type = Cell.EMPTY;
         }
      }

      // Apply rules.
      w = 1;
      h = 1;

      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            count = 0;
            x2    = x - w;

            while (x2 < 0)
            {
               x2 += Parameters.ORGANISM_DIMENSIONS.width;
            }

            y2 = y;

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            x2 = x;
            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            x2 = x + w;

            while (x2 >= Parameters.ORGANISM_DIMENSIONS.width)
            {
               x2 -= Parameters.ORGANISM_DIMENSIONS.width;
            }

            y2 = y;

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }


            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            if (predecessorCells[x][y].type != Cell.EMPTY)
            {
               if ((count > 3) || (count < 2))
               {
                  cells[x][y].type = Cell.EMPTY;
               }
               else
               {
                  cells[x][y].type = 0;
               }
            }
            else
            {
               if (count == 3)
               {
                  cells[x][y].type = 0;
               }
               else
               {
                  cells[x][y].type = Cell.EMPTY;
               }
            }
         }
      }
   }
}
