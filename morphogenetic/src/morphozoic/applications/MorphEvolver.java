// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Random;
import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Parameters;

// Morph evolver application.
public class MorphEvolver
{
   // Options.
   public static final String OPTIONS =
      "\n\t[-organismDimensions <width> <height> (# cells)]"
      + "\n\t[-numCellTypes <number of cell types>]"
      + "\n\t[-populationSize <size>]"
      + "\n\t[-fitPopulationSize <number of fit members retained per generation>]"
      + "\n\t[-numEvolveEpochs <number of morph evolve epochs>]"
      + "\n\t[-numEpochUpdateSteps <number of update steps per epoch>]"
      + "\n\t[-numGenerations <number of generations per epoch>]"
      + "\n\t[-numMatingOffspring <number of offspring from matings>]"
      + "\n\t[-resume (resume optimization)]"
      + "\n\t[-randomSeed <random seed>]";

   // Evolution parameters:

   // Population size.
   public static int POPULATION_SIZE = 5;

   // Fit population size.
   public static int FIT_POPULATION_SIZE = 2;

   // Number of morph evolve epochs.
   public static int NUM_EVOLVE_EPOCHS = 1;

   // Epoch update steps.
   public static int EPOCH_UPDATE_STEPS = 3;

   // Number of generations per epoch.
   public static int NUM_GENERATIONS = 1;
   public static int FIT_GENERATIONS = -1;

   // Number offspring from matings.
   public static int NUM_MATING_OFFSPRING = 2;

   // File names.
   public static final String MEMBER_FILE_NAME_PREFIX = "morph_member";
   public static final String FITTEST_FILE_NAME       = "morph_fittest.dat";

   // Morph population member.
   class MorphMember
   {
      // Organism.
      Gastrulation organism;

      // Fitness.
      float fitness;

      // Member file name.
      String filename;

      // Run history.
      ArrayList<Float> fitnessHistory;
      ArrayList < ArrayList < Metamorph >> usageHistory;

      // Constructors.
      MorphMember(String filename, boolean resume) throws IOException
      {
         this.filename = filename;
         organism      = null;
         fitness       = 0.0f;

         // Resuming?
         if (resume)
         {
            loadParms();
         }
         else
         {
            // Initialize.
            DataOutputStream writer = null;
            try
            {
               writer = new DataOutputStream(new FileOutputStream(filename));
               Parameters.save(writer);
               for (Metamorph metamorph : generateMetamorphs())
               {
                  metamorph.save(writer);
               }
            }
            catch (Exception e)
            {
               System.err.println("Cannot save file " + filename +
                                  ":" + e.getMessage());
               throw new IOException("Cannot save file " + filename +
                                     ":" + e.getMessage());
            }
            finally
            {
               if (writer != null)
               {
                  writer.close();
               }
            }
         }
      }


      MorphMember(Gastrulation organism)
      {
         filename      = null;
         this.organism = organism;
         fitness       = 0.0f;
      }


      // Generate metamorphs.
      ArrayList<Metamorph> generateMetamorphs() throws IOException
      {
         ArrayList<Metamorph> metamorphs = new ArrayList<Metamorph>();
         for (Metamorph source : fitnessMetamorphs)
         {
            Metamorph metamorph = source.clone();
            Metamorph target    = fitnessMetamorphs.get(randomizer.nextInt(fitnessMetamorphs.size()));
            metamorph.targetCells = target.cloneTargetCells();
            metamorphs.add(metamorph);
         }
         return(metamorphs);
      }


      // Load parameters.
      void loadParms() throws IOException
      {
         DataInputStream reader = null;

         try
         {
            reader = new DataInputStream(new FileInputStream(filename));
            Parameters.load(reader);
         }
         catch (Exception e)
         {
            System.err.println("Cannot load parameters from file " + filename +
                               ":" + e.getMessage());
            throw new IOException("Cannot load parameters from file " + filename +
                                  ":" + e.getMessage());
         }
         finally
         {
            if (reader != null)
            {
               reader.close();
            }
         }
      }


      // Run.
      void run(Gastrulation fitnessTarget, int steps) throws IOException
      {
         // Update.
         fitnessHistory = new ArrayList<Float>();
         usageHistory   = new ArrayList < ArrayList < Metamorph >> ();
         fitnessTarget.clearCells();
         fitness       = 0.0f;
         String[] args = new String[2];
         args[0]       = "-execMetamorphs";
         args[1]       = filename;
         organism      = new Gastrulation(args, 0);
         for (int i = 0; i < steps; i++)
         {
            organism.update();
            fitnessTarget.update();
            float f = 0.0f;
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  if (fitnessTarget.cells[x][y].type == Cell.EMPTY)
                  {
                     if (organism.cells[x][y].type == Cell.EMPTY)
                     {
                        f += 1.0f;
                     }
                  }
                  else if (organism.cells[x][y].type != Cell.EMPTY)
                  {
                     f += 1.0f;
                  }
               }
            }
            fitness += f;
            fitnessHistory.add(f);
            ArrayList<Metamorph> usage = new ArrayList<Metamorph>();
            usageHistory.add(usage);
            for (Metamorph metamorph : organism.metamorphs)
            {
               if (metamorph.usage)
               {
                  usage.add(metamorph);
               }
            }
         }

         // Print cells.
         printCells();
      }


      // Create mutant of member.
      MorphMember mutate(String filename) throws IOException
      {
         // Create mutant.
         MorphMember member = new MorphMember(filename, false);

         DataOutputStream writer = null;

         try
         {
            writer = new DataOutputStream(new FileOutputStream(member.filename));
            Parameters.save(writer);

            // Create mutant metamorph.
            Metamorph mutableMetamorph = null;
            Metamorph mutantMetamorph  = null;
            float     maxFitness       = (float)(Parameters.ORGANISM_DIMENSIONS.width * Parameters.ORGANISM_DIMENSIONS.height);
            for (int i = 0, j = fitnessHistory.size(); i < j &&
                 mutableMetamorph == null && fitnessMetamorphs.size() > 0; i++)
            {
               if (fitnessHistory.get(i) < maxFitness)
               {
                  ArrayList<Metamorph> usedMetamorphs = usageHistory.get(i);
                  int n = usedMetamorphs.size();
                  if (n > 0)
                  {
                     mutableMetamorph = usedMetamorphs.get(randomizer.nextInt(n));
                     mutantMetamorph  = mutableMetamorph.clone();
                     int       t      = randomizer.nextInt(fitnessMetamorphs.size());
                     Metamorph target = fitnessMetamorphs.get(t);
                     mutantMetamorph.targetCells = target.cloneTargetCells();
                  }
               }
            }

            for (Metamorph metamorph : organism.metamorphs)
            {
               if (mutableMetamorph == metamorph)
               {
                  mutantMetamorph.save(writer);
               }
               else
               {
                  metamorph.save(writer);
               }
            }
         }
         catch (Exception e)
         {
            System.err.println("Cannot save file " + member.filename +
                               ":" + e.getMessage());
            throw new IOException("Cannot save file " + member.filename +
                                  ":" + e.getMessage());
         }
         finally
         {
            if (writer != null)
            {
               writer.close();
            }
         }
         return(member);
      }


      // Mate parents to create offspring.
      MorphMember mate(MorphMember parent, String filename) throws IOException
      {
         // Create offspring.
         MorphMember member = new MorphMember(filename, false);

         DataOutputStream writer = null;

         try
         {
            writer = new DataOutputStream(new FileOutputStream(member.filename));
            Parameters.save(writer);
            for (int i = 0, j = organism.metamorphs.size(); i < j; i++)
            {
               if (randomizer.nextBoolean())
               {
                  organism.metamorphs.get(i).save(writer);
               }
               else
               {
                  parent.organism.metamorphs.get(i).save(writer);
               }
            }
         }
         catch (Exception e)
         {
            System.err.println("Cannot save file " + member.filename +
                               ":" + e.getMessage());
            throw new IOException("Cannot save file " + member.filename +
                                  ":" + e.getMessage());
         }
         finally
         {
            if (writer != null)
            {
               writer.close();
            }
         }
         return(member);
      }


      // Print cells.
      void printCells()
      {
         if (organism != null)
         {
            for (int y = Parameters.ORGANISM_DIMENSIONS.height - 1; y >= 0; y--)
            {
               for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
               {
                  if (organism.cells[x][y].type == Cell.EMPTY)
                  {
                     System.out.print("_");
                  }
                  else
                  {
                     System.out.print(organism.cells[x][y].type + "");
                  }
               }
               System.out.println();
            }
         }
      }
   }

   // Morph population.
   ArrayList<MorphMember> population;

   // Random numbers.
   Random randomizer;

   // Fitness target.
   Gastrulation         fitnessTarget;
   MorphMember          fitnessMorph;
   ArrayList<Metamorph> fitnessMetamorphs;

   // Constructor.
   public MorphEvolver(boolean resume) throws IllegalArgumentException, IOException
   {
      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Create fitness target and metamorphs.
      String[] args = new String[2];
      args[0]       = "-genMetamorphs";
      args[1]       = MEMBER_FILE_NAME_PREFIX + "_target.dat";
      fitnessTarget = new Gastrulation(args, 0);
      for (int i = 0, j = NUM_GENERATIONS * EPOCH_UPDATE_STEPS; i < j; i++)
      {
         fitnessTarget.update();
      }
      fitnessMetamorphs = fitnessTarget.metamorphs;
      fitnessTarget     = new Gastrulation(new String[0], 0);
      fitnessMorph      = new MorphMember(fitnessTarget);

      // Create population.
      population = new ArrayList<MorphMember>();
      for (int i = 0; i < POPULATION_SIZE; i++)
      {
         MorphMember member = new MorphMember(MEMBER_FILE_NAME_PREFIX + "_" + i + ".dat", resume);
         population.add(member);
      }
   }


   // Run optimization.
   public void run() throws IOException
   {
      for (int i = 0; i < NUM_EVOLVE_EPOCHS; i++)
      {
         System.out.println("Evolve epoch=" + i);

         // Update target.
         for (int j = 0; j < EPOCH_UPDATE_STEPS; j++)
         {
            fitnessTarget.update();
         }
         System.out.println("Fitness target:");
         fitnessMorph.printCells();

         int fitGeneration = -1;
         for (int j = 0; j < NUM_GENERATIONS || FIT_GENERATIONS != -1; j++)
         {
            if (j < NUM_GENERATIONS)
            {
               System.out.println("Generation=" + (j + 1) + "/" + NUM_GENERATIONS);
            }
            else
            {
               System.out.println("Generation=" + (j + 1));
            }

            // Run members.
            System.out.println("Member\tFitness");
            int steps = ((i + 1) * EPOCH_UPDATE_STEPS) + 1;
            for (int k = 0; k < population.size(); k++)
            {
               MorphMember member = population.get(k);
               member.run(fitnessTarget, steps);
               System.out.println(k + "\t" + member.fitness);
            }

            // Select fit members.
            System.out.println("Fit:");
            ArrayList<MorphMember> nextPopulation       = new ArrayList<MorphMember>(population.size());
            ArrayList<Integer>     fitPopulationIndexes = new ArrayList<Integer>();
            for (int k = 0; k < POPULATION_SIZE; k++)
            {
               nextPopulation.add(null);
            }
            for (int k = 0; k < FIT_POPULATION_SIZE; k++)
            {
               int   n = -1;
               float f = 0.0f;
               for (int p = 0; p < POPULATION_SIZE; p++)
               {
                  MorphMember member = population.get(p);
                  if ((member != null) && ((n == -1) || (member.fitness > f)))
                  {
                     n = p;
                     f = member.fitness;
                  }
               }
               nextPopulation.set(n, population.get(n));
               population.set(n, null);
               fitPopulationIndexes.add(n);
               System.out.println(n + "");
            }
            int s = fitPopulationIndexes.size();

            // Mate fit members.
            System.out.println("Mate:");
            for (int k = 0, n = 0; k < POPULATION_SIZE &&
                 FIT_POPULATION_SIZE > 1 && n < NUM_MATING_OFFSPRING; k++)
            {
               MorphMember member = nextPopulation.get(k);
               if (member == null)
               {
                  int         n1      = fitPopulationIndexes.get(randomizer.nextInt(s));
                  int         n2      = -1;
                  MorphMember parent1 = nextPopulation.get(n1);
                  MorphMember parent2 = null;
                  while (parent2 == null)
                  {
                     n2 = fitPopulationIndexes.get(randomizer.nextInt(s));
                     if (n1 != n2)
                     {
                        parent2 = nextPopulation.get(n2);
                     }
                  }
                  nextPopulation.set(k, parent1.mate(parent2, MEMBER_FILE_NAME_PREFIX + "_" + k + ".dat"));
                  System.out.println(n1 + "+" + n2 + "->" + k);
                  n++;
               }
            }

            // Mutate.
            System.out.println("Mutate:");
            for (int k = 0; k < POPULATION_SIZE; k++)
            {
               if (nextPopulation.get(k) == null)
               {
                  int         n      = fitPopulationIndexes.get(randomizer.nextInt(s));
                  MorphMember member = nextPopulation.get(n);
                  nextPopulation.set(k, member.mutate(MEMBER_FILE_NAME_PREFIX + "_" + k + ".dat"));
                  System.out.println(n + "->" + k);
               }
            }
            population = nextPopulation;

            // Fit members established?
            if (FIT_GENERATIONS != -1)
            {
               if (fitGeneration == -1)
               {
                  float f = (float)(Parameters.ORGANISM_DIMENSIONS.width * Parameters.ORGANISM_DIMENSIONS.height);
                  f *= (float)steps;
                  for (int k = 0; k < POPULATION_SIZE; k++)
                  {
                     if (population.get(k).fitness == f)
                     {
                        fitGeneration = j;
                        break;
                     }
                  }
               }
               if (fitGeneration != -1)
               {
                  if ((j - fitGeneration) >= FIT_GENERATIONS)
                  {
                     break;
                  }
               }
            }
         }
      }

      System.out.println("Fittest:\nMember\tFitness");
      int         fittestIndex  = -1;
      MorphMember fittestMember = null;
      for (int i = 0; i < population.size(); i++)
      {
         MorphMember member = population.get(i);
         if ((fittestIndex == -1) || (member.fitness > fittestMember.fitness))
         {
            fittestIndex  = i;
            fittestMember = member;
         }
      }
      if (fittestIndex != -1)
      {
         System.out.println(fittestIndex + "\t" + fittestMember.fitness);
         fittestMember.printCells();
         System.out.println("Parameters:");
         Parameters.print();
         Files.copy(new File(fittestMember.filename).toPath(),
                    new File(FITTEST_FILE_NAME).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
         System.out.println("Organism saved to " + FITTEST_FILE_NAME);
      }
      else
      {
         System.out.println("Unavailable");
      }
   }


   // Main.
   public static void main(String[] args)
   {
      String  usage  = "Usage: java morphozoic.applications.MorphEvolver" + OPTIONS;
      boolean resume = false;

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
         else if (args[i].equals("-fitPopulationSize"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            FIT_POPULATION_SIZE = Integer.parseInt(args[i]);
            if (FIT_POPULATION_SIZE <= 0)
            {
               System.err.println("Fit population size must be positive");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numEvolveEpochs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_EVOLVE_EPOCHS = Integer.parseInt(args[i]);
            if (NUM_EVOLVE_EPOCHS < 0)
            {
               System.err.println("Invalid number of morph evolve epochs");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numEpochUpdateSteps"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            EPOCH_UPDATE_STEPS = Integer.parseInt(args[i]);
            if (EPOCH_UPDATE_STEPS < 0)
            {
               System.err.println("Invalid number of morph evolve epoch update steps");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numGenerations"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_GENERATIONS = Integer.parseInt(args[i]);
            if (NUM_GENERATIONS < 0)
            {
               System.err.println("Invalid nummber of generations");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-numMatingOffspring"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            NUM_MATING_OFFSPRING = Integer.parseInt(args[i]);
            if (NUM_MATING_OFFSPRING < 0)
            {
               System.err.println("Invalid number of mating offspring");
               System.err.println(usage);
               return;
            }
         }
         else if (args[i].equals("-resume"))
         {
            resume = true;
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
         else if (args[i].equals("-fitGenerations"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            FIT_GENERATIONS = Integer.parseInt(args[i]);
            if (FIT_GENERATIONS < 0)
            {
               System.err.println("Invalid fit generations");
               return;
            }
         }
         else
         {
            System.out.println(usage);
            return;
         }
      }
      if (FIT_POPULATION_SIZE > POPULATION_SIZE)
      {
         System.err.println("Fit population size cannot exceed population size");
         System.err.println(usage);
         return;
      }
      if (NUM_MATING_OFFSPRING > (POPULATION_SIZE - FIT_POPULATION_SIZE))
      {
         System.err.println("Too many mating offspring");
         System.err.println(usage);
         return;
      }
      if (Parameters.METAMORPH_RANDOM_BIAS > 0.0f)
      {
         Parameters.METAMORPH_RANDOM_BIAS = 0.0f;
         System.out.println("Setting METAMORPH_RANDOM_BIAS = " + Parameters.METAMORPH_RANDOM_BIAS);
      }
      if (Parameters.MAX_CELL_METAMORPHS > 1)
      {
         Parameters.MAX_CELL_METAMORPHS = 1;
         System.out.println("Setting MAX_CELL_METAMORPHS = " + Parameters.MAX_CELL_METAMORPHS);
      }
      if (Parameters.INHIBIT_COMPETING_MORPHOGENS)
      {
         Parameters.INHIBIT_COMPETING_MORPHOGENS = false;
         System.out.println("Setting INHIBIT_COMPETING_MORPHOGENS = false");
      }

      try
      {
         MorphEvolver evolver = new MorphEvolver(resume);
         evolver.run();
      }
      catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }
}
