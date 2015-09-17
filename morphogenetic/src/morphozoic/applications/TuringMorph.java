// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import rdtree.RDclient;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Parameters;

// Coloration.
public class TuringMorph extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.TuringMorph";
   public static boolean      RANDOMIZE_CELLS_BEFORE_EXECUTION = false;

   // Options.
   public static final String OPTIONS =
      "\n\t[-genMetamorphs <save file name>]"
      + "\n\t[-execMetamorphs <load file name> (overrides command-line parameters)]"
      + "\n\t[-randomizeCellsBeforeExecution (randomize initial cells before executing metamorphs)]";

   // Constructor.
   public TuringMorph(String[] args, Integer id) throws IllegalArgumentException, IOException
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
         else if (args[i].equals("-randomizeCellsBeforeExecution"))
         {
            RANDOMIZE_CELLS_BEFORE_EXECUTION = true;
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
      if ((execFilename == null) && RANDOMIZE_CELLS_BEFORE_EXECUTION)
      {
         System.err.println("Randomizing cells before execution does not apply");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      // Initialize Turing morph.
      Random turingRandomizer = new Random(randomizer.nextInt());
      if (RANDOMIZE_CELLS_BEFORE_EXECUTION) { turingRandomizer.nextDouble(); }
      turingInit(turingRandomizer);

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
      // Initialize update.
      initUpdate();

      // Update cells.
      if ((execFilename == null) || (tick == 0))
      {
         // Do Turing morph.
         int n = iterations;
         if (tick == 0) { n = 1; }
         double[][] values = turingMorph(n);

         // Find range of concentrations.
         double high = Double.NEGATIVE_INFINITY;
         double low  = Double.POSITIVE_INFINITY;
         for (int i = 0; i < height; ++i)
         {
            for (int j = 0; j < width; ++j)
            {
               double val = values[i][j];
               if (val > high) { high = val; }
               else if (val < low) { low = val; }
            }
         }

         // Scale values to number of cell types.
         n = Parameters.NUM_CELL_TYPES;
         for (int i = 0; i < height; ++i)
         {
            for (int j = 0; j < width; ++j)
            {
               int scaled = (int)((values[i][j] - low) * (double)n / (high - low));
               scaled           = Math.max(0, Math.min(n - 1, scaled));
               cells[i][j].type = scaled;
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


   /**
    * Turing's reaction-diffusion model.
    * For more information, see:
    *   [1] Rafael Collantes. Algorithm Alley. Dr. Dobb's Journal, December 1996.
    *   [2] Alan M. Turing. The chemical basis of morphogenesis. Philosophical
    *       Transactions of the Royal Society of London. B 327, 37-72 (1952)
    *
    * @author Christopher G. Jennings (cjennings [at] acm.org)
    * www.cgjennings.ca/toybox/turingmorph/
    */
   double CA         = 3.5d;
   double CB         = 16d;
   int    iterations = 500;
   int    width, height;
   double[][] Ao, An, Bo, Bn;

   void turingInit(Random randomizer)
   {
      width  = Parameters.ORGANISM_DIMENSIONS.width;
      height = Parameters.ORGANISM_DIMENSIONS.height;
      Ao     = new double[width][height];
      An     = new double[width][height];
      Bo     = new double[width][height];
      Bn     = new double[width][height];
      randomize(randomizer);
   }


   double[][] turingMorph(int iterations)
   {
      int    n, i, j, iplus1, iminus1, jplus1, jminus1;
      double DiA, ReA, DiB, ReB;

      // Use Euler's method to solve the differential equations.
      for (n = 0; n < iterations; ++n)
      {
         for (i = 0; i < height; ++i)
         {
            // Treat the surface as a torus by wrapping at the edges.
            iplus1  = i + 1;
            iminus1 = i - 1;
            if (i == 0) { iminus1 = height - 1; }
            if (i == height - 1) { iplus1 = 0; }

            for (j = 0; j < width; ++j)
            {
               jplus1  = j + 1;
               jminus1 = j - 1;
               if (j == 0) { jminus1 = width - 1; }
               if (j == width - 1) { jplus1 = 0; }

               // Component A.
               DiA = CA * (Ao[iplus1][j] - 2.0 * Ao[i][j] + Ao[iminus1][j]
                           + Ao[i][jplus1] - 2.0 * Ao[i][j] + Ao[i][jminus1]);
               ReA      = Ao[i][j] * Bo[i][j] - Ao[i][j] - 12.0;
               An[i][j] = Ao[i][j] + 0.01 * (ReA + DiA);
               if (An[i][j] < 0.0) { An[i][j] = 0.0; }

               // Component B.
               DiB = CB * (Bo[iplus1][j] - 2.0 * Bo[i][j] + Bo[iminus1][j]
                           + Bo[i][jplus1] - 2.0 * Bo[i][j] + Bo[i][jminus1]);
               ReB      = 16.0 - Ao[i][j] * Bo[i][j];
               Bn[i][j] = Bo[i][j] + 0.01 * (ReB + DiB);
               if (Bn[i][j] < 0.0) { Bn[i][j] = 0.0; }
            }
         }

         // Swap Ao for An, Bo for Bn.
         swapBuffers();
      }
      return(An);
   }


   /**
    * Set the system to an initial noise state.
    * This should normally called before solving.
    */
   void randomize(Random randomizer)
   {
      for (int i = 0; i < height; ++i)
      {
         for (int j = 0; j < width; ++j)
         {
            Ao[i][j] = randomizer.nextDouble() * 12.0 + randomizer.nextGaussian() * 2.0;
            Bo[i][j] = randomizer.nextDouble() * 12.0 + randomizer.nextGaussian() * 2.0;
         }
      }
   }


   private void swapBuffers()
   {
      double[][] temp = Ao;
      Ao   = An;
      An   = temp;
      temp = Bo;
      Bo   = Bn;
      Bn   = temp;
   }
}
