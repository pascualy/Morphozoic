// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Morphogen;
import morphozoic.Organism;

// Gastrulation.
public class Gastrulation extends Organism
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
   public Gastrulation(String[] args, Integer randomSeed) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism morphozoic.applications.Gastrulation]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

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
            System.err.println("Cannot save file " + genFilename +
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
      }
   }


   @Override
   public void update()
   {
      int x, y, x2, y2, s, s2;

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
                           parent = sameParents.get(0);
                        }
                        else if (otherParents.size() > 0)
                        {
                           parent = otherParents.get(0);
                        }
                        if (parent != null)
                        {
                           Cell clone = cells[x][y].clone();
                           clone.x -= parent.x;
                           clone.y -= parent.y;
                           saveMetamorph(Metamorph.division(parent.morphogen, clone));
                        }
                     }
                     else
                     {
                        if (cells[x][y].type != predecessorCells[x][y].type)
                        {
                           // Type change.
                           saveMetamorph(Metamorph.type(predecessorCells[x][y].morphogen, cells[x][y].type));
                        }
                        else if (cells[x][y].orientation != predecessorCells[x][y].orientation)
                        {
                           // Orientation change.
                           saveMetamorph(Metamorph.orientation(predecessorCells[x][y].morphogen, cells[x][y].orientation));
                        }
                        else
                        {
                           // Stasis.
                           saveMetamorph(Metamorph.stasis(predecessorCells[x][y].morphogen));
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
                        saveMetamorph(Metamorph.death(predecessorCells[x][y].morphogen));
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


   // Save metamorph.
   private void saveMetamorph(Metamorph metamorph) throws IOException
   {
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
}
