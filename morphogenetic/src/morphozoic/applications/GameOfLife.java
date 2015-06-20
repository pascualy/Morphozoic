// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import morphozoic.Cell;
import morphozoic.Metamorph;
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
            writer.writeInt(Cell.numTypes);
            writer.flush();
         }
         catch (Exception e)
         {
            throw new IOException("Cannot open save file " + genFilename +
                                  ":" + e.getMessage());
         }
      }
      if (execFilename != null)
      {
         isEditable = false;
         try {
            reader = new DataInputStream(new FileInputStream(execFilename));
            int n = reader.readInt();
            if (n != Cell.numTypes)
            {
               throw new IOException("Cell numTypes (" + n + ") in file " + execFilename +
                                     " must equal cell numTypes (" + Cell.numTypes + ")");
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
            throw new IOException("Cannot load file " + execFilename +
                                  ":" + e.getMessage());
         }
      }
   }


   @Override
   public void update()
   {
      int x, y, x2, y2;

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
         // Execute metamorphs.
         for (x = 0; x < DIMENSIONS.width; x++)
         {
            for (y = 0; y < DIMENSIONS.height; y++)
            {
               if (predecessorCells[x][y].type != Cell.EMPTY)
               {
                  for (Metamorph morph : metamorphs)
                  {
                     if (predecessorCells[x][y].morphogen.equals(morph.morphogen))
                     {
                        morph.exec(cells[x][y]);
                     }
                  }
               }
            }
         }
      }
      tick++;

      // Create metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         try
         {
            // Save metamorphs.
            Vector<Cell> sameParents  = new Vector<Cell>();
            Vector<Cell> otherParents = new Vector<Cell>();
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
               {
                  if (cells[x][y].type != Cell.EMPTY)
                  {
                     if (predecessorCells[x][y].type == Cell.EMPTY)
                     {
                        // Determine parent of new cell.
                        for (x2 = x - 1; x2 <= x + 1; x2++)
                        {
                           for (y2 = y - 1; y2 <= y + 1; y2++)
                           {
                              if ((x2 == x) && (y2 == y)) { continue; }
                              if ((x2 < 0) || (x2 >= DIMENSIONS.width)) { continue; }
                              if ((y2 < 0) || (y2 >= DIMENSIONS.height)) { continue; }
                              if (predecessorCells[x2][y2].type != Cell.EMPTY)
                              {
                                 if (predecessorCells[x2][y2].type == cells[x][y].type)
                                 {
                                    sameParents.add(predecessorCells[x2][y2]);
                                 }
                                 else
                                 {
                                    otherParents.add(predecessorCells[x2][y2]);
                                 }
                              }
                           }
                        }
                        Cell parent = null;
                        if (sameParents.size() > 0)
                        {
                           // Prefer to divide parent of same type.
                           parent = sameParents.get(randomizer.nextInt(sameParents.size()));
                        }
                        else if (otherParents.size() > 0)
                        {
                           parent = otherParents.get(randomizer.nextInt(otherParents.size()));
                        }
                        if (parent != null)
                        {
                           Cell clone = cells[x][y].clone();
                           clone.x -= parent.x;
                           clone.y -= parent.y;
                           Metamorph.division(parent.morphogen, clone).save(writer);
                        }
                     }
                     else
                     {
                        if (cells[x][y].type != predecessorCells[x][y].type)
                        {
                           // Type change.
                           Metamorph.type(predecessorCells[x][y].morphogen, cells[x][y].type).save(writer);
                        }
                        else if (cells[x][y].orientation != predecessorCells[x][y].orientation)
                        {
                           // Orientation change.
                           Metamorph.orientation(predecessorCells[x][y].morphogen, cells[x][y].orientation).save(writer);
                        }
                     }
                  }
               }
            }
            for (x = 0; x < DIMENSIONS.width; x++)
            {
               for (y = 0; y < DIMENSIONS.height; y++)
               {
                  if (cells[x][y].type == Cell.EMPTY)
                  {
                     if (predecessorCells[x][y].type != Cell.EMPTY)
                     {
                        Metamorph.death(predecessorCells[x][y].morphogen).save(writer);
                     }
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
