// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// Parameters.
public class Parameters
{
   // Organism dimensions in cell units.
   public static final Dimension DEFAULT_ORGANISM_DIMENSIONS = new Dimension(50, 50);
   public static Dimension       ORGANISM_DIMENSIONS         = DEFAULT_ORGANISM_DIMENSIONS;

   // Number of cell types.
   public static final int DEFAULT_NUM_CELL_TYPES = 3;
   public static int       NUM_CELL_TYPES         = DEFAULT_NUM_CELL_TYPES;

   // Neighborhood dimension: odd number.
   public static final int DEFAULT_NEIGHBORHOOD_DIMENSION = 3;
   public static int       NEIGHBORHOOD_DIMENSION         = DEFAULT_NEIGHBORHOOD_DIMENSION;

   // Number of neighborhoods.
   public static final int DEFAULT_NUM_NEIGHBORHOODS = 3;
   public static int       NUM_NEIGHBORHOODS         = DEFAULT_NUM_NEIGHBORHOODS;

   // Nested neighborhood importance weights.
   // Weight array of size NUM_NEIGHBORHOODS summing to 1.
   public static float[] NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = null;

   // Maximum morphogen comparison distance.
   // Morphogens matching neighborhoods further than this distance are ignored.
   public static final float DEFAULT_MAX_MORPHOGEN_COMPARE_DISTANCE = 1000.0f;
   public static float       MAX_MORPHOGEN_COMPARE_DISTANCE         = DEFAULT_MAX_MORPHOGEN_COMPARE_DISTANCE;

   // Metamorph dimension: odd number.
   public static final int DEFAULT_METAMORPH_DIMENSION = 3;
   public static int       METAMORPH_DIMENSION         = DEFAULT_METAMORPH_DIMENSION;

   // Maximum number of metamorphs matching cell morphogen.
   // For weighted metamorph selection.
   public static final int DEFAULT_MAX_CELL_METAMORPHS = 3;
   public static int       MAX_CELL_METAMORPHS         = DEFAULT_MAX_CELL_METAMORPHS;

   // Metamorph selection randomness bias.
   // 0 = least random.
   public static final float DEFAULT_METAMORPH_RANDOM_BIAS = 0.001f;
   public static float       METAMORPH_RANDOM_BIAS         = DEFAULT_METAMORPH_RANDOM_BIAS;

   // Probabilistically morph cells?
   public static final boolean DEFAULT_PROBABILISTIC_METAMORPH = true;
   public static boolean       PROBABILISTIC_METAMORPH         = DEFAULT_PROBABILISTIC_METAMORPH;

   // Inhibit competing morphogens?
   public static boolean INHIBIT_COMPETING_MORPHOGENS = true;

   // Morphogenetic cell dispersion.
   public static final int DEFAULT_MORPHOGENETIC_CELL_DISPERSION_MODULO = 1;
   public static int       MORPHOGENETIC_CELL_DISPERSION_MODULO         = DEFAULT_MORPHOGENETIC_CELL_DISPERSION_MODULO;

   // Metamorph execution options.
   public static enum METAMORPH_EXEC_OPTION
   {
      LINEAR_SEARCH,
      SEARCH_TREE,
      NEURAL_NETWORK
   }
   public static final METAMORPH_EXEC_OPTION DEFAULT_METAMORPH_EXEC_TYPE = METAMORPH_EXEC_OPTION.NEURAL_NETWORK;
   public static METAMORPH_EXEC_OPTION       METAMORPH_EXEC_TYPE         = DEFAULT_METAMORPH_EXEC_TYPE;

   // Default organism.
   public static final String DEFAULT_ORGANISM = "morphozoic.applications.Gastrulation";

   // Random seed.
   public static final int DEFAULT_RANDOM_SEED = 4517;
   public static int       RANDOM_SEED         = DEFAULT_RANDOM_SEED;

   // Save parameters.
   public static void save(DataOutputStream writer) throws IOException
   {
      writer.writeInt(ORGANISM_DIMENSIONS.width);
      writer.writeInt(ORGANISM_DIMENSIONS.height);
      writer.writeInt(NUM_CELL_TYPES);
      writer.writeInt(NEIGHBORHOOD_DIMENSION);
      writer.writeInt(NUM_NEIGHBORHOODS);
      if (NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS != null)
      {
         writer.writeInt(NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length);
         for (int i = 0; i < NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length; i++)
         {
            writer.writeFloat(NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[i]);
         }
      }
      else
      {
         writer.writeInt(-1);
      }
      writer.writeFloat(MAX_MORPHOGEN_COMPARE_DISTANCE);
      writer.writeInt(METAMORPH_DIMENSION);
      writer.writeInt(MAX_CELL_METAMORPHS);
      writer.writeFloat(METAMORPH_RANDOM_BIAS);
      writer.writeBoolean(PROBABILISTIC_METAMORPH);
      writer.writeBoolean(INHIBIT_COMPETING_MORPHOGENS);
      writer.writeInt(MORPHOGENETIC_CELL_DISPERSION_MODULO);
      switch (METAMORPH_EXEC_TYPE)
      {
      case LINEAR_SEARCH:
         writer.writeInt(0);
         break;

      case SEARCH_TREE:
         writer.writeInt(1);
         break;

      case NEURAL_NETWORK:
         writer.writeInt(2);
         break;
      }
      writer.writeInt(RANDOM_SEED);
      writer.flush();
   }


   // Load parameters.
   public static void load(DataInputStream reader) throws IOException
   {
      ORGANISM_DIMENSIONS.width  = reader.readInt();
      ORGANISM_DIMENSIONS.height = reader.readInt();
      NUM_CELL_TYPES             = reader.readInt();
      NEIGHBORHOOD_DIMENSION     = reader.readInt();
      NUM_NEIGHBORHOODS          = reader.readInt();
      int n = reader.readInt();
      if (n != -1)
      {
         NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = new float[n];
         for (int i = 0; i < NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length; i++)
         {
            NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[i] = reader.readFloat();
         }
      }
      else
      {
         NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = null;
      }
      MAX_MORPHOGEN_COMPARE_DISTANCE       = reader.readFloat();
      METAMORPH_DIMENSION                  = reader.readInt();
      MAX_CELL_METAMORPHS                  = reader.readInt();
      METAMORPH_RANDOM_BIAS                = reader.readFloat();
      PROBABILISTIC_METAMORPH              = reader.readBoolean();
      INHIBIT_COMPETING_MORPHOGENS         = reader.readBoolean();
      MORPHOGENETIC_CELL_DISPERSION_MODULO = reader.readInt();
      n = reader.readInt();
      switch (n)
      {
      case 0:
         METAMORPH_EXEC_TYPE = METAMORPH_EXEC_OPTION.LINEAR_SEARCH;
         break;

      case 1:
         METAMORPH_EXEC_TYPE = METAMORPH_EXEC_OPTION.SEARCH_TREE;
         break;

      case 2:
         METAMORPH_EXEC_TYPE = METAMORPH_EXEC_OPTION.NEURAL_NETWORK;
         break;
      }
      RANDOM_SEED = reader.readInt();
   }


   // Print parameters.
   public static void print()
   {
      System.out.println("ORGANISM_DIMENSIONS = width:" + ORGANISM_DIMENSIONS.width + " height:" + ORGANISM_DIMENSIONS.height);
      System.out.println("NUM_CELL_TYPES = " + NUM_CELL_TYPES);
      System.out.println("NEIGHBORHOOD_DIMENSION = " + NEIGHBORHOOD_DIMENSION);
      System.out.println("NUM_NEIGHBORHOODS = " + NUM_NEIGHBORHOODS);
      System.out.print("NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS = ");
      if (NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS != null)
      {
         for (int i = 0; i < NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS.length; i++)
         {
            System.out.print(NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[i] + " ");
         }
         System.out.println();
      }
      else
      {
         System.out.println("null");
      }
      System.out.println("MAX_MORPHOGEN_COMPARE_DISTANCE = " + MAX_MORPHOGEN_COMPARE_DISTANCE);
      System.out.println("METAMORPH_DIMENSION = " + METAMORPH_DIMENSION);
      System.out.println("MAX_CELL_METAMORPHS = " + MAX_CELL_METAMORPHS);
      System.out.println("METAMORPH_RANDOM_BIAS = " + METAMORPH_RANDOM_BIAS);
      System.out.println("PROBABILISTIC_METAMORPH = " + PROBABILISTIC_METAMORPH);
      System.out.println("INHIBIT_COMPETING_MORPHOGENS = " + INHIBIT_COMPETING_MORPHOGENS);
      System.out.println("MORPHOGENETIC_CELL_DISPERSION_MODULO = " + MORPHOGENETIC_CELL_DISPERSION_MODULO);
      System.out.print("METAMORPH_EXEC_TYPE = ");
      switch (METAMORPH_EXEC_TYPE)
      {
      case LINEAR_SEARCH:
         System.out.println("LINEAR_SEARCH");
         break;

      case SEARCH_TREE:
         System.out.println("SEARCH_TREE");
         break;

      case NEURAL_NETWORK:
         System.out.println("NEURAL_NETWORK");
         break;
      }
      System.out.println("DEFAULT_ORGANISM = " + DEFAULT_ORGANISM);
      System.out.println("RANDOM_SEED = " + RANDOM_SEED);
   }
}
