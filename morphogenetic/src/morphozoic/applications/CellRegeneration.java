// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.util.Random;
import rdtree.RDclient;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Parameters;

// Cell regeneration demonstration.
public class CellRegeneration extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.CellRegeneration";

   // Options.
   public static final String OPTIONS =
      "\n\t[-testBar <\"horizontal\" or \"vertical\"]";

   // Metamorph execution options.
   public static enum TEST_BAR_TYPE
   {
      HORIZONTAL_BAR,
      VERTICAL_BAR
   }
   public static final TEST_BAR_TYPE DEFAULT_TEST_BAR = TEST_BAR_TYPE.HORIZONTAL_BAR;
   public TEST_BAR_TYPE              TEST_BAR         = DEFAULT_TEST_BAR;

   // Constructor.
   public CellRegeneration(String[] args, Integer id) throws Exception
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Check parameters.
      if ((Parameters.ORGANISM_DIMENSIONS.width < 17) ||
          (Parameters.ORGANISM_DIMENSIONS.height < 17))
      {
         System.err.println("Organism width and height must be >= 17");
         throw new IllegalArgumentException("Organism width and height must be >= 17");
      }
      if (Parameters.NUM_CELL_TYPES < 3)
      {
         System.err.println("Number of cell types must be >= 3");
         throw new IllegalArgumentException("Number of cell types must be >= 3");
      }

      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-testBar"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               throw new IllegalArgumentException(usage);
            }
            if (args[i].equals("horizontal"))
            {
               TEST_BAR = TEST_BAR_TYPE.HORIZONTAL_BAR;
            }
            else if (args[i].equals("vertical"))
            {
               TEST_BAR = TEST_BAR_TYPE.VERTICAL_BAR;
            }
            else
            {
               System.err.println("Invalid testBar option");
               throw new IllegalArgumentException("Invalid testBar option");
            }
         }
         else
         {
            System.err.println(usage);
            throw new IllegalArgumentException(usage);
         }
      }

      // Clear cells.
      markCells(2);

      // Common values.
      d    = 3;
      minr = d / 2;
      maxr = d + minr;
      cx   = Parameters.ORGANISM_DIMENSIONS.width / 2;
      cy   = Parameters.ORGANISM_DIMENSIONS.height / 2;
      minx = 0;
      maxx = Parameters.ORGANISM_DIMENSIONS.width - 1;
      miny = 0;
      maxy = Parameters.ORGANISM_DIMENSIONS.height - 1;

      // Create configuration: horizontal border & vertical bar.
      markHorizontalBorders(1);
      markVerticalBar(0);

      // Generate metamorphs.
      generateMetamorphs();
      markHorizontalBorders(2);
      markVerticalBar(2);

      // Create configuration: vertical border & horizontal bar.
      markVerticalBorders(1);
      markHorizontalBar(0);

      // Generate metamorphs.
      generateMetamorphs();
      markVerticalBorders(2);
      markHorizontalBar(2);

      // Create test bar.
      if (TEST_BAR == TEST_BAR_TYPE.HORIZONTAL_BAR)
      {
         markVerticalBorders(1);
      }
      else
      {
         markHorizontalBorders(1);
      }
      markCenterBar(0);
   }


   // Common values.
   int d;
   int minr;
   int maxr;
   int cx;
   int cy;
   int minx;
   int maxx;
   int miny;
   int maxy;

   // Mark cells with cell type.
   void markCells(int type)
   {
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y].type = type;
         }
      }
   }


   // Mark border with cell type.
   void markBorder(int type)
   {
      markHorizontalBorders(type);
      markVerticalBorders(type);
   }


   // Mark horizontal borders with cell type.
   void markHorizontalBorders(int type)
   {
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if ((y >= miny) && (y < (miny + d)))
            {
               if ((x >= minx) && (x <= maxx))
               {
                  cells[x][y].type = type;
               }
            }
            else if ((y <= maxy) && (y > (maxy - d)))
            {
               if ((x >= minx) && (x <= maxx))
               {
                  cells[x][y].type = type;
               }
            }
         }
      }
   }


   // Mark vertical borders with cell type.
   void markVerticalBorders(int type)
   {
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if ((x >= minx) && (x < (minx + d)))
            {
               if ((y >= miny) && (y <= maxy))
               {
                  cells[x][y].type = type;
               }
            }
            else if ((x <= maxx) && (x > (maxx - d)))
            {
               if ((y >= miny) && (y <= maxy))
               {
                  cells[x][y].type = type;
               }
            }
         }
      }
   }


   // Mark cross pattern with cell type.
   void markCross(int type)
   {
      markHorizontalBar(type);
      markVerticalBar(type);
   }


   // Mark horizontal bar with cell type.
   void markHorizontalBar(int type)
   {
      for (int x = cx - maxr; x <= cx + maxr; x++)
      {
         for (int y = cy - maxr; y <= cy + maxr; y++)
         {
            if ((y <= (cy + minr)) && (y >= (cy - minr)))
            {
               cells[x][y].type = type;
            }
         }
      }
   }


   // Mark vertical bar with cell type.
   void markVerticalBar(int type)
   {
      for (int x = cx - maxr; x <= cx + maxr; x++)
      {
         for (int y = cy - maxr; y <= cy + maxr; y++)
         {
            if ((x <= (cx + minr)) && (x >= (cx - minr)))
            {
               cells[x][y].type = type;
            }
         }
      }
   }


   // Mark center bar with cell type.
   void markCenterBar(int type)
   {
      for (int x = cx - maxr; x <= cx + maxr; x++)
      {
         for (int y = cy - maxr; y <= cy + maxr; y++)
         {
            if ((x <= (cx + minr)) && (x >= (cx - minr)))
            {
               if ((y <= (cy + minr)) && (y >= (cy - minr)))
               {
                  cells[x][y].type = type;
               }
            }
         }
      }
   }


   // Mark box pattern with cell type.
   void markBox(int type)
   {
      for (int x = cx - maxr; x <= cx + maxr; x++)
      {
         for (int y = cy - maxr; y <= cy + maxr; y++)
         {
            boolean mark = true;
            if ((x >= (cx - minr)) && (x <= (cx + minr)))
            {
               if ((y >= (cy - minr)) && (y <= (cy + minr)))
               {
                  mark = false;
               }
            }
            if (mark)
            {
               cells[x][y].type = type;
            }
         }
      }
   }


   // Generate metamorphs.
   void generateMetamorphs() throws Exception
   {
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            boolean morph = false;
            if ((x >= (cx - maxr)) && (x <= (cx + maxr)))
            {
               if ((y >= (cy - maxr)) && (y <= (cy + maxr)))
               {
                  morph = true;
               }
            }
            if (morph)
            {
               cells[x][y].generateMorphogen();
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
            else
            {
               cells[x][y].morphogen = null;
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
   }


   @Override
   public void update()
   {
      // Initialize update.
      initUpdate();
      int d    = 3;
      int minr = d / 2;
      int maxr = d + minr;
      int cx   = Parameters.ORGANISM_DIMENSIONS.width / 2;
      int cy   = Parameters.ORGANISM_DIMENSIONS.height / 2;
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            boolean delete = true;
            if ((x >= (cx - maxr)) && (x <= (cx + maxr)))
            {
               if ((y <= (cy + minr)) && (y >= (cy - minr)))
               {
                  delete = false;
               }
            }
            if ((y >= (cy - maxr)) && (y <= (cy + maxr)))
            {
               if ((x <= (cx + minr)) && (x >= (cx - minr)))
               {
                  delete = false;
               }
            }
            if (delete)
            {
               predecessorCells[x][y].morphogen = null;
            }
         }
      }

      // Execute metamorphs.
      execMetamorphs();

      tick++;
   }
}
