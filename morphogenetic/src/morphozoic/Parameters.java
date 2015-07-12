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

   // Morphogen distance bias.
   // For weighted metamorph selection.
   public static final float DEFAULT_MORPHOGEN_DISTANCE_BIAS = 0.001f;
   public static float       MORPHOGEN_DISTANCE_BIAS         = DEFAULT_MORPHOGEN_DISTANCE_BIAS;

   // Probabilistically morph cells?
   public static final boolean DEFAULT_PROBABILISTIC_METAMORPH = true;
   public static boolean       PROBABILISTIC_METAMORPH         = DEFAULT_PROBABILISTIC_METAMORPH;

   // Inhibit competing morphogens?
   public static boolean INHIBIT_COMPETING_MORPHOGENS = true;

   // Morphogenetic cell dispersion.
   public static final int DEFAULT_MORPHOGENETIC_DISPERSION_MODULO = 1;
   public static int       MORPHOGENETIC_DISPERSION_MODULO         = DEFAULT_MORPHOGENETIC_DISPERSION_MODULO;

   // Default organism.
   public static final String DEFAULT_ORGANISM = "morphozoic.applications.Gastrulation";

   // Random seed.
   public static final int DEFAULT_RANDOM_SEED = 4517;
   public static int       RANDOM_SEED         = DEFAULT_RANDOM_SEED;

   // Save structural parameters.
   public static void saveParms(DataOutputStream writer) throws IOException
   {
      writer.writeInt(ORGANISM_DIMENSIONS.width);
      writer.writeInt(ORGANISM_DIMENSIONS.height);
      writer.writeInt(NUM_CELL_TYPES);
      writer.writeInt(NEIGHBORHOOD_DIMENSION);
      writer.writeInt(METAMORPH_DIMENSION);
      writer.writeInt(NUM_NEIGHBORHOODS);
      writer.writeInt(MORPHOGENETIC_DISPERSION_MODULO);
      writer.flush();
   }


   // Load and check structural parameters.
   public static void loadParms(DataInputStream reader) throws IOException
   {
      int n = reader.readInt();

      if (n != ORGANISM_DIMENSIONS.width)
      {
         throw new IOException("Organism dimensions width (" + n + ") loaded must equal dimensions width (" + ORGANISM_DIMENSIONS.width + ")");
      }
      n = reader.readInt();
      if (n != ORGANISM_DIMENSIONS.height)
      {
         throw new IOException("Organism dimensions height (" + n + ") loaded must equal dimensions height (" + ORGANISM_DIMENSIONS.height + ")");
      }
      n = reader.readInt();
      if (n != NUM_CELL_TYPES)
      {
         throw new IOException("Number of cell types (" + n + ") loaded must equal number of cell types (" + NUM_CELL_TYPES + ")");
      }
      n = reader.readInt();
      if (n != NEIGHBORHOOD_DIMENSION)
      {
         throw new IOException("Morphogen neighborhood dimension (" + n + ") loaded must equal neighborhood dimension (" + NEIGHBORHOOD_DIMENSION + ")");
      }
      n = reader.readInt();
      if (n != METAMORPH_DIMENSION)
      {
         throw new IOException("Metamorph dimension (" + n + ") loaded must equal metamorph dimension (" + METAMORPH_DIMENSION + ")");
      }
      n = reader.readInt();
      if (n != NUM_NEIGHBORHOODS)
      {
         throw new IOException("Morphogen number of neighborhoods (" + n + ") loaded must equal number of neighborhoods (" + NUM_NEIGHBORHOODS + ")");
      }
      n = reader.readInt();
      if (n != MORPHOGENETIC_DISPERSION_MODULO)
      {
         throw new IOException("Morphogenetic locale dispersion (" + n + ") loaded must equal morphogenetic local dispersion (" + MORPHOGENETIC_DISPERSION_MODULO + ")");
      }
   }
}
