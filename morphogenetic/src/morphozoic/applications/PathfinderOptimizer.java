// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import morphozoic.Cell;
import morphozoic.Parameters;

// Optimize path finder application.
public class PathfinderOptimizer
{
   // Options.
   public static final String OPTIONS = "\n\t[-organismDimensions <width> <height> (# cells)]\n\t[-numCellTypes <number of cell types>]\n\t[-randomSeed <random seed>]\n\t[-populationSize <size>]\n\t[-numTrainingMorphs <number of training morphs>]\n\t[-numSourceCells <number of source cells>]\n\t[-numTargetCells <number of target cells>]\n\t[-numUpdateSteps <number of morph update steps>]";

   // Optimization parameters:

   // Population size.
   public static int POPULATION_SIZE = 5;

   // Number of training morphs.
   public static int NUM_TRAINING_MORPHS = 10;

   // Number of source cells.
   public static int NUM_SOURCE_CELLS = 2;

   // Number of target cells.
   public static int NUM_TARGET_CELLS = 2;

   // Number of update steps.
   public static int NUM_UPDATE_STEPS = 20;

   // Neighborhood dimension range: odd number.
   public static final int MIN_NEIGHBORHOOD_DIMENSION = 3;
   public static final int MAX_NEIGHBORHOOD_DIMENSION = 5;

   // Number of neighborhoods range.
   public static final int MIN_NUM_NEIGHBORHOODS = 2;
   public static final int MAX_NUM_NEIGHBORHOODS = 2;

   // Metamorph dimension range: odd number.
   public static final int MIN_METAMORPH_DIMENSION = 3;
   public static final int MAX_METAMORPH_DIMENSION = 3;

   // Maximum number of metamorphs matching cell morphogen range.
   public static final int MIN_MAX_CELL_METAMORPHS = 1;
   public static final int MAX_MAX_CELL_METAMORPHS = 3;

   // Metamorph selection randomness bias range.
   public static final float MIN_METAMORPH_RANDOM_BIAS   = 0.0f;
   public static final float MAX_METAMORPH_RANDOM_BIAS   = 0.01f;
   public static final float DELTA_METAMORPH_RANDOM_BIAS = 0.001f;

   // Metamorph file name.
   public static final String METAMORPH_FILE_NAME = "pathfinder_metamorphs.dat";

   // Path finder population member.
   class PathfinderMember
   {
      // Path finder.
      Pathfinder pathfinder;

      // Fitness.
      float fitness;

      // Parameters:

      // Neighborhood dimension: odd number.
      int NEIGHBORHOOD_DIMENSION;

      // Number of neighborhoods.
      int NUM_NEIGHBORHOODS;

      // Nested neighborhood importance weights.
      // Weight array of size NUM_NEIGHBORHOODS summing to 1.
      float[] NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS;

      // Metamorph dimension: odd number.
      int METAMORPH_DIMENSION;

      // Maximum number of metamorphs matching cell morphogen.
      // For weighted metamorph selection.
      int MAX_CELL_METAMORPHS;

      // Metamorph selection randomness bias.
      // 0 = least random.
      float METAMORPH_RANDOM_BIAS;

      // Inhibit competing morphogens?
      boolean INHIBIT_COMPETING_MORPHOGENS;

      void run(Pathfinder target) throws IOException
      {
         // Set parameters.
         Parameters.NEIGHBORHOOD_DIMENSION = NEIGHBORHOOD_DIMENSION;
         Parameters.NUM_NEIGHBORHOODS      = NUM_NEIGHBORHOODS;
         Parameters.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS;
         Parameters.METAMORPH_DIMENSION          = METAMORPH_DIMENSION;
         Parameters.MAX_CELL_METAMORPHS          = MAX_CELL_METAMORPHS;
         Parameters.METAMORPH_RANDOM_BIAS        = METAMORPH_RANDOM_BIAS;
         Parameters.INHIBIT_COMPETING_MORPHOGENS = INHIBIT_COMPETING_MORPHOGENS;

         // Train.
         Random r = new Random(trainingSeed);
         String[] args = new String[2];
         args[0]       = "-genMetamorphs";
         args[1]       = METAMORPH_FILE_NAME;
         for (int i = 0; i < NUM_TRAINING_MORPHS; i++)
         {
            pathfinder = new Pathfinder(args, 0);
            args[0]    = "-accumMetamorphs";
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  pathfinder.cells[x][y].type = Cell.EMPTY;
               }
            }
            for (int j = 0; j < NUM_SOURCE_CELLS; j++)
            {
               pathfinder.cells[0][r.nextInt(Parameters.ORGANISM_DIMENSIONS.height)].type = Pathfinder.SOURCE_CELL;
            }
            for (int j = 0; j < NUM_TARGET_CELLS; j++)
            {
               pathfinder.cells[Parameters.ORGANISM_DIMENSIONS.width - 1][r.nextInt(Parameters.ORGANISM_DIMENSIONS.height)].type = Pathfinder.TARGET_CELL;
            }
            for (int j = 0; j < NUM_UPDATE_STEPS; j++)
            {
               pathfinder.update();
            }
         }

         // Test.
         args[0]             = "-execMetamorphs";
         args[1]             = METAMORPH_FILE_NAME;
         pathfinder          = new Pathfinder(args, 0);
         int[][] shadowTypes = new int[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               pathfinder.cells[x][y].type = shadowTypes[x][y] = Cell.EMPTY;
            }
         }
         r = new Random(testingSeed);
         for (int i = 0; i < NUM_SOURCE_CELLS; i++)
         {
            int y = r.nextInt(Parameters.ORGANISM_DIMENSIONS.height);
            pathfinder.cells[0][y].type = Pathfinder.SOURCE_CELL;
            shadowTypes[0][y]           = Pathfinder.SOURCE_CELL;
         }
         for (int i = 0; i < NUM_TARGET_CELLS; i++)
         {
            int y = r.nextInt(Parameters.ORGANISM_DIMENSIONS.height);
            pathfinder.cells[Parameters.ORGANISM_DIMENSIONS.width - 1][y].type = Pathfinder.TARGET_CELL;
            shadowTypes[Parameters.ORGANISM_DIMENSIONS.width - 1][y]           = Pathfinder.TARGET_CELL;
         }
         for (int i = 0; i < NUM_UPDATE_STEPS; i++)
         {
            pathfinder.update();

            // Constrain morphs to branching from source cells.
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  switch (shadowTypes[x][y])
                  {
                  case Pathfinder.SOURCE_CELL:
                  case Pathfinder.TARGET_CELL:
                     if ((pathfinder.cells[x][y].type == Cell.EMPTY) ||
                         (pathfinder.cells[x][y].type == Pathfinder.BRANCH_CELL))
                     {
                        pathfinder.cells[x][y].type = shadowTypes[x][y];
                     }
                     break;

                  case Cell.EMPTY:
                  case Pathfinder.BRANCH_CELL:
                     if ((pathfinder.cells[x][y].type == Pathfinder.SOURCE_CELL) ||
                         (pathfinder.cells[x][y].type == Pathfinder.TARGET_CELL))
                     {
                        pathfinder.cells[x][y].type = Cell.EMPTY;
                     }
                     break;
                  }
                  if (pathfinder.cells[x][y].type == Pathfinder.BRANCH_CELL)
                  {
                     boolean branchOK = false;
                     int     x2       = x - 1;
                     int     y2       = y - 1;
                     for (int x3 = 0; x3 < 3; x3++)
                     {
                        for (int y3 = 0; y3 < 3; y3++)
                        {
                           if ((x3 == 1) && (y3 == 1)) { continue; }
                           if ((x2 + x3) < Parameters.ORGANISM_DIMENSIONS.width)
                           {
                              int x4 = Pathfinder.wrapX(x2 + x3);
                              int y4 = Pathfinder.wrapY(y2 + y3);
                              if ((pathfinder.cells[x4][y4].type == Pathfinder.SOURCE_CELL) ||
                                  (pathfinder.cells[x4][y4].type == Pathfinder.BRANCH_CELL))
                              {
                                 branchOK = true;
                              }
                           }
                        }
                     }
                     if (!branchOK)
                     {
                        pathfinder.cells[x][y].type = Cell.EMPTY;
                     }
                  }
               }
            }
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  shadowTypes[x][y] = pathfinder.cells[x][y].type;
               }
            }
         }

         // Evaluate fitness.
         fitness = 0.0f;
         int b = 0;
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               if (pathfinder.cells[x][y].type == Pathfinder.SOURCE_CELL)
               {
                  for (int x2 = 0; x2 < Parameters.ORGANISM_DIMENSIONS.width; x2++)
                  {
                     for (int y2 = 0; y2 < Parameters.ORGANISM_DIMENSIONS.height; y2++)
                     {
                        if (pathfinder.cells[x2][y2].type == Pathfinder.TARGET_CELL)
                        {
                           if (pathFound(pathfinder.cells, x, y, x2, y2,
                                         new boolean[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height]))
                           {
                              fitness += 1.0f;
                           }
                        }
                     }
                  }
               }
               else if (pathfinder.cells[x][y].type == Pathfinder.BRANCH_CELL)
               {
                  b++;
               }
            }
         }
         float d = (float)(Parameters.ORGANISM_DIMENSIONS.width * Parameters.ORGANISM_DIMENSIONS.height);
         fitness += (d - (float)b) / d;

         // Print.
         System.out.println("Target:");
         printCA(target.cells);
         System.out.println("Test:");
         printCA(pathfinder.cells);
      }


      // Path found from source to target?
      boolean pathFound(Cell[][] cells, int sx, int sy, int tx, int ty, boolean[][] searched)
      {
         if ((sx == tx) && (sy == ty))
         {
            return(true);
         }
         searched[sx][sy] = true;
         int x = sx - 1;
         int y = sy - 1;
         for (int x2 = 0; x2 < 3; x2++)
         {
            for (int y2 = 0; y2 < 3; y2++)
            {
               if ((x + x2) >= 0)
               {
                  int x3 = Pathfinder.wrapX(x + x2);
                  int y3 = Pathfinder.wrapY(y + y2);
                  if (!searched[x3][y3] && (cells[x3][y3].type != Cell.EMPTY))
                  {
                     if (pathFound(cells, x3, y3, tx, ty, searched))
                     {
                        return(true);
                     }
                  }
               }
            }
         }
         return(false);
      }


      // Print cells.
      void printCA(Cell[][] cells)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               if (cells[x][y].type == Cell.EMPTY)
               {
                  System.out.print("_");
               }
               else
               {
                  System.out.print(cells[x][y].type + "");
               }
            }
            System.out.println();
         }
      }
   }

   // Path finder population.
   ArrayList<PathfinderMember> population;

   // Random numbers.
   Random randomizer;
   int    trainingSeed;
   int    testingSeed;

   // Constructor.
   public PathfinderOptimizer() throws IllegalArgumentException
   {
      // Random numbers.
      randomizer   = new Random(Parameters.RANDOM_SEED);
      trainingSeed = randomizer.nextInt();
      testingSeed  = randomizer.nextInt();

      // Create path finder population.
      population = new ArrayList<PathfinderMember>();
      for (int i = 0; i < POPULATION_SIZE; i++)
      {
         PathfinderMember member = new PathfinderMember();
         population.add(member);
         member.pathfinder = null;
         member.fitness    = 0.0f;

         // Generate member parameters.
         if (MAX_NEIGHBORHOOD_DIMENSION == MIN_NEIGHBORHOOD_DIMENSION)
         {
            member.NEIGHBORHOOD_DIMENSION = MAX_NEIGHBORHOOD_DIMENSION;
         }
         else
         {
            member.NEIGHBORHOOD_DIMENSION = randomizer.nextInt(MAX_NEIGHBORHOOD_DIMENSION - MIN_NEIGHBORHOOD_DIMENSION) + MIN_NEIGHBORHOOD_DIMENSION;
            if ((member.NEIGHBORHOOD_DIMENSION % 2) != 1)
            {
               if (randomizer.nextBoolean())
               {
                  member.NEIGHBORHOOD_DIMENSION--;
               }
               else
               {
                  member.NEIGHBORHOOD_DIMENSION++;
               }
            }
         }
         if (MAX_NUM_NEIGHBORHOODS == MIN_NUM_NEIGHBORHOODS)
         {
            member.NUM_NEIGHBORHOODS = MAX_NUM_NEIGHBORHOODS;
         }
         else
         {
            member.NUM_NEIGHBORHOODS = randomizer.nextInt(MAX_NUM_NEIGHBORHOODS - MIN_NUM_NEIGHBORHOODS) + MIN_NUM_NEIGHBORHOODS;
         }
         member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = new float[member.NUM_NEIGHBORHOODS];
         float f = 0.0f;
         for (int j = 0; j < member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length; j++)
         {
            member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[j] = randomizer.nextFloat();
            f += member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[j];
         }
         for (int j = 0; j < member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length; j++)
         {
            if (f > 0.0f)
            {
               member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[j] /= f;
            }
            else
            {
               member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[j] = 1.0f / member.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length;
            }
         }
         if (MAX_METAMORPH_DIMENSION == MIN_METAMORPH_DIMENSION)
         {
            member.METAMORPH_DIMENSION = MAX_METAMORPH_DIMENSION;
         }
         else
         {
            member.METAMORPH_DIMENSION = randomizer.nextInt(MAX_METAMORPH_DIMENSION - MIN_METAMORPH_DIMENSION) + MIN_METAMORPH_DIMENSION;
            if ((member.METAMORPH_DIMENSION % 2) != 1)
            {
               if (randomizer.nextBoolean())
               {
                  member.METAMORPH_DIMENSION--;
               }
               else
               {
                  member.METAMORPH_DIMENSION++;
               }
            }
         }
         member.MAX_CELL_METAMORPHS          = randomizer.nextInt(MAX_MAX_CELL_METAMORPHS - MIN_MAX_CELL_METAMORPHS) + MIN_MAX_CELL_METAMORPHS;
         member.METAMORPH_RANDOM_BIAS        = (randomizer.nextFloat() * (MAX_METAMORPH_RANDOM_BIAS - MIN_METAMORPH_RANDOM_BIAS)) + MIN_METAMORPH_RANDOM_BIAS;
         member.INHIBIT_COMPETING_MORPHOGENS = randomizer.nextBoolean();
      }
   }


   // Run optimization.
   public void run() throws IOException
   {
      // Create testing target.
      Pathfinder target = new Pathfinder(new String[0], 0);
      Random     r      = new Random(testingSeed);

      for (int i = 0; i < NUM_SOURCE_CELLS; i++)
      {
         target.cells[0][r.nextInt(Parameters.ORGANISM_DIMENSIONS.height)].type = Pathfinder.SOURCE_CELL;
      }
      for (int i = 0; i < NUM_TARGET_CELLS; i++)
      {
         target.cells[Parameters.ORGANISM_DIMENSIONS.width - 1][r.nextInt(Parameters.ORGANISM_DIMENSIONS.height)].type = Pathfinder.TARGET_CELL;
      }
      for (int i = 0; i < NUM_UPDATE_STEPS; i++)
      {
         target.update();
      }

      // Run members.
      System.out.println("Member\tFitness");
      for (int i = 0; i < population.size(); i++)
      {
         PathfinderMember member = population.get(i);
         member.run(target);
         System.out.println(i + "\t" + member.fitness);
      }
   }


   // Main.
   public static void main(String[] args)
   {
      String usage = "Usage: java morphozoic.applications.PathfinderOptimizer" + OPTIONS + "\n\t[organism-specific options]";

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-organismDimensions"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int w = Integer.parseInt(args[i]);
            if (w <= 0)
            {
               System.err.println("Organism width dimension must be positive");
               System.err.println(usage);
               return;
            }
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            int h = Integer.parseInt(args[i]);
            if (h <= 0)
            {
               System.err.println("Organism height dimension must be positive");
               System.err.println(usage);
               return;
            }
            Parameters.ORGANISM_DIMENSIONS = new Dimension(w, h);
         }
         else if (args[i].equals("-numCellTypes"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.NUM_CELL_TYPES = Integer.parseInt(args[i]);
            if (Parameters.NUM_CELL_TYPES <= 0)
            {
               System.err.println("Number of cell types must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            Parameters.RANDOM_SEED = Integer.parseInt(args[i]);
         }
         else if (args[i].equals("-populationSize"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            POPULATION_SIZE = Integer.parseInt(args[i]);
            if (POPULATION_SIZE <= 0)
            {
               System.err.println("Population size must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numTrainingMorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_TRAINING_MORPHS = Integer.parseInt(args[i]);
            if (NUM_TRAINING_MORPHS < 0)
            {
               System.err.println("Invalid number of training morphs");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numSourceCells"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_SOURCE_CELLS = Integer.parseInt(args[i]);
            if (NUM_SOURCE_CELLS <= 0)
            {
               System.err.println("Number of source cells must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numTargetCells"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_TARGET_CELLS = Integer.parseInt(args[i]);
            if (NUM_TARGET_CELLS <= 0)
            {
               System.err.println("Number of target cells must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numUpdateSteps"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_UPDATE_STEPS = Integer.parseInt(args[i]);
            if (NUM_UPDATE_STEPS < 0)
            {
               System.err.println("Invalid number of update steps");
               System.err.println(usage);
               return;
            }
         }
         else
         {
            System.out.println(usage);
            return;
         }
      }

      if (Parameters.NUM_CELL_TYPES != 3)
      {
         System.err.println("Number of cell types must equal 3");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      try
      {
         PathfinderOptimizer optimizer = new PathfinderOptimizer();
         optimizer.run();
      }
      catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }
}
