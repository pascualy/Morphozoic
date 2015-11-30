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

// Game of Life completeness test.
public class GameOfLifeTest extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.GameOfLifeTest";

   // Options.
   public static final String OPTIONS =
      "\n\t[-genMetamorphs <save file name>]"
      + "\n\t[-execMetamorphs <load file name> (overrides command-line parameters)]";

   // Game of Life cell states.
   public static final int DEAD  = 1;
   public static final int ALIVE = 0;

   // Constructor.
   public GameOfLifeTest(String[] args, Integer id) throws IllegalArgumentException, IOException
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
      if ((execFilename == null) && (Parameters.NUM_CELL_TYPES != 2))
      {
         System.err.println("Number of cell types must equal 2");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      isEditable = true;
      if (genFilename != null)
      {
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               cells[x][y].type = DEAD;
            }
         }
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
                  cells[x][y].type = DEAD;
               }
            }
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  cells[x][y].type = ALIVE;
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
      // This will generate all possible 3x3 neighborhood values as tick runs from 0 to 511.
      if (tick == 512)
      {
         System.exit(0);                        // Only need 0-511
      }
      int bits = tick;
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y].type = bits & 0x1; //  ALIVE=0, DEAD=1
            bits             = bits >> 1;
         }
      }

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
                  if (predecessorCells[x][y].type == ALIVE)
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

      if (execFilename == null)
      {
         // Step Game of Life.
         step();
      }
      else
      {
         Cell[][] startconfig = copyGrid(predecessorCells);
         System.out.println("Start: ");
         printGrid(startconfig);

         //create the correct output using the step function
         step();
         System.out.println("Step: ");
         printGrid(cells);

         //save the correct output
         Cell[][] correctconfig = copyGrid(cells);

         //reset the grid to its original state
         cells = copyGrid(startconfig);

         //rerun with metamorphs
         execMetamorphs();
         System.out.println("Morph: ");
         printGrid(cells);

         //check if grids are equal if they are not output to stdout
         boolean ok = true;
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width && ok; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height && ok; y++)
            {
               if (correctconfig[x][y].type != cells[x][y].type)
               {
                  System.out.println("Incorrect output");
                  ok = false;
               }
            }
         }
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


   Cell[][] copyGrid(Cell[][] original)
   {
      if (original == null)
      {
         return(null);
      }
      final Cell[][] result = new Cell[3][3];
      for (int i = 0; i < 3; i++)
      {
         for (int j = 0; j < 3; j++)
         {
            result[i][j] = original[i][j].clone();
         }
      }
      return(result);
   }


   void printGrid(Cell[][] output)
   {
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.height; x++)
      {
         String line = "";
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.width; y++)
         {
            line += output[x][y].type;
         }
         System.out.println(line);
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
            cells[x][y].type = DEAD;
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

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            x2 = x;
            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            x2 = x + w;

            while (x2 >= Parameters.ORGANISM_DIMENSIONS.width)
            {
               x2 -= Parameters.ORGANISM_DIMENSIONS.width;
            }

            y2 = y;

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += Parameters.ORGANISM_DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= Parameters.ORGANISM_DIMENSIONS.height)
            {
               y2 -= Parameters.ORGANISM_DIMENSIONS.height;
            }


            if (predecessorCells[x2][y2].type == ALIVE)
            {
               count++;
            }

            if (predecessorCells[x][y].type == ALIVE)
            {
               if ((count > 3) || (count < 2))
               {
                  cells[x][y].type = DEAD;
               }
               else
               {
                  cells[x][y].type = ALIVE;
               }
            }
            else
            {
               if (count == 3)
               {
                  cells[x][y].type = ALIVE;
               }
               else
               {
                  cells[x][y].type = DEAD;
               }
            }
         }
      }
   }
}
