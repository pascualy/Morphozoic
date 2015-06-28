// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.Dimension;
import java.util.Random;

// Organism.
public class Organism
{
   // Default organism.
   public static final String DEFAULT_ORGANISM = "morphozoic.applications.Gastrulation";

   // Random seed.
   public static final int DEFAULT_RANDOM_SEED = 4517;
   public int              randomSeed          = DEFAULT_RANDOM_SEED;
   public Random           randomizer;

   // Dimensions in cell units.
   public Dimension DIMENSIONS = new Dimension(50, 50);

   // Cells.
   public Cell[][] cells;

   // Cells editable?
   public boolean isEditable = false;

   // Update ticks.
   public int tick;

   // Metamorph files.
   public String genFilename  = null;
   public String execFilename = null;

   // Metamorph morphogen distance.
   public class MetamorphDistance
   {
      public float     morphogenDistance;
      public Metamorph metamorph;

      public MetamorphDistance(float d, Metamorph m)
      {
         morphogenDistance = d;
         metamorph         = m;
      }
   }

   // Constructors.
   public Organism(String[] args, Integer randomSeed)
   {
      this.randomSeed = randomSeed;
      init();
   }


   public Organism()
   {
      init();
   }


   // Initialize.
   private void init()
   {
      int x, y;

      // Random numbers.
      randomizer = new Random(randomSeed);

      // Create cells.
      cells = new Cell[DIMENSIONS.width][DIMENSIONS.height];

      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            cells[x][y] = new Cell(Cell.EMPTY, x, y, Orientation.NORTH, this);
         }
      }

      tick = 0;
   }


   // Update.
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

      tick++;
   }
}