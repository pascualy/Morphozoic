// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Morphogen;
import morphozoic.Organism;

// Game of Life.
public class GameOfLife extends Organism
{
   // Metamorphs.
   public Vector<Metamorph> metamorphs;

   // Predecessor cells.
   public Cell[][] predecessorCells;

   // Options.
   public static final String OPTIONS = "\n\t[-genMetamorphs <save file name>]\n\t[-execMetamorphs <load file name>]";

   // Metamorph data streams.
   private DataOutputStream writer;
   private DataInputStream  reader;

   // Constructor.
   public GameOfLife(String[] args, Integer randomSeed) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism morphozoic.applications.GameOfLife]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-genMetamorphs"))
         {
            i++;
            genFilename = args[i];
         }
         else if (args[i].equals("-execMetamorphs"))
         {
            i++;
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
      tick             = 0;
      isEditable       = true;
      metamorphs       = new Vector<Metamorph>();
      predecessorCells = new Cell[DIMENSIONS.width][DIMENSIONS.height];
      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            writer.writeInt(Cell.NUM_TYPES);
            writer.writeInt(Morphogen.NUM_SPHERES);
            writer.writeInt(Morphogen.SECTOR_DIMENSION);
            writer.flush();
         }
         catch (Exception e)
         {
            System.err.println("Cannot open save file " + genFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot open save file " + genFilename +
                                  ":" + e.getMessage());
         }
      }
      if (execFilename != null)
      {
         try {
            reader = new DataInputStream(new FileInputStream(execFilename));
            int n = reader.readInt();
            if (n != Cell.NUM_TYPES)
            {
               throw new IOException("Cell numTypes (" + n + ") in file " + execFilename +
                                     " must equal cell numTypes (" + Cell.NUM_TYPES + ")");
            }
            n = reader.readInt();
            if (n != Morphogen.NUM_SPHERES)
            {
               throw new IOException("Morphogen numSpheres (" + n + ") in file " + execFilename +
                                     " must equal numSpheres (" + Morphogen.NUM_SPHERES + ")");
            }
            n = reader.readInt();
            if (n != Morphogen.SECTOR_DIMENSION)
            {
               throw new IOException("Morphogen sectorDimension (" + n + ") in file " + execFilename +
                                     " must equal sectorDimension (" + Morphogen.SECTOR_DIMENSION + ")");
            }
            int     x, y;
            boolean eof = false;
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
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
               while ((m = Metamorph.load(reader)) != null)
               {
                  metamorphs.add(m);
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
      int x, y;

      // Generate morphogenetic fields.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            cells[x][y].generateMorphogen();
         }
      }

      // Create predecessor cells.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            predecessorCells[x][y]           = cells[x][y].clone();
            predecessorCells[x][y].morphogen = cells[x][y].morphogen;
            cells[x][y].morphogen            = null;
         }
      }

      // Save initial cell types?
      if ((tick == 0) && (genFilename != null))
      {
         try
         {
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
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
         // Match metamorphs to cell morphogens.
         MetamorphDistance cellMorphs[][] = new MetamorphDistance[DIMENSIONS.width][DIMENSIONS.height];
         for (x = 0; x < DIMENSIONS.width; x++)
         {
            for (y = 0; y < DIMENSIONS.height; y++)
            {
               if (predecessorCells[x][y].type != Cell.EMPTY)
               {
                  float dist = 0.0f;
                  for (Metamorph m : metamorphs)
                  {
                     float d = predecessorCells[x][y].morphogen.compare(m.morphogen);
                     if ((cellMorphs[x][y] == null) || (d < dist))
                     {
                        cellMorphs[x][y] = new MetamorphDistance(d, m);
                        dist             = d;
                     }
                  }
               }
            }
         }

         // Execute metamorphs by descending morphogen distance.
         boolean exec = true;
         while (exec)
         {
            exec = false;
            float     dist  = -1.0f;
            Metamorph morph = null;
            int       cx    = 0;
            int       cy    = 0;
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
               {
                  MetamorphDistance m = cellMorphs[x][y];
                  if (m != null)
                  {
                     if ((dist < 0.0f) || (m.morphogenDistance > dist))
                     {
                        dist  = m.morphogenDistance;
                        morph = m.metamorph;
                        cx    = x;
                        cy    = y;
                     }
                  }
               }
            }
            if (morph != null)
            {
               morph.exec(cells[cx][cy]);
               cellMorphs[cx][cy] = null;
               exec = true;
            }
         }
      }
      tick++;
      if (genFilename != null)
      {
         isEditable = false;
      }

      // Create metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         try
         {
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
               {
                  if (predecessorCells[x][y].type != Cell.EMPTY)
                  {
                     saveMetamorph(predecessorCells[x][y].morphogen, cells[x][y]);
                  }
               }
            }
         }
         catch (IOException e)
         {
            System.err.println("Cannot save metamorphs to " + genFilename + ":" + e.getMessage());
         }
      }
   }


   // Save metamorph.
   private void saveMetamorph(Morphogen morphogen, Cell cell) throws IOException
   {
      Metamorph metamorph = new Metamorph(morphogen, cell);

      for (Metamorph m : metamorphs)
      {
         if (m.equals(metamorph))
         {
            return;
         }
      }
      metamorphs.add(metamorph);
      metamorph.save(writer);
   }


   // Step Game of Life.
   private void step()
   {
      int x, y, x2, y2, w, h, count;

      // Clear cells.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            cells[x][y].type = Cell.EMPTY;
         }
      }

      // Apply rules.
      w = 1;
      h = 1;

      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            count = 0;
            x2    = x - w;

            while (x2 < 0)
            {
               x2 += DIMENSIONS.width;
            }

            y2 = y;

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= DIMENSIONS.height)
            {
               y2 -= DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            x2 = x;
            y2 = y - h;

            while (y2 < 0)
            {
               y2 += DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= DIMENSIONS.height)
            {
               y2 -= DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            x2 = x + w;

            while (x2 >= DIMENSIONS.width)
            {
               x2 -= DIMENSIONS.width;
            }

            y2 = y;

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y - h;

            while (y2 < 0)
            {
               y2 += DIMENSIONS.height;
            }

            if (predecessorCells[x2][y2].type != Cell.EMPTY)
            {
               count++;
            }

            y2 = y + h;

            while (y2 >= DIMENSIONS.height)
            {
               y2 -= DIMENSIONS.height;
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
