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
import morphozoic.Organism.MetamorphDistance;

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
         isEditable = true;
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
}
