// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

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
   public static final Dimension DEFAULT_DIMENSIONS = new Dimension(50, 50);
   public static Dimension       DIMENSIONS         = DEFAULT_DIMENSIONS;

   // Morphogenetic locale dispersion.
   public static final int DEFAULT_MORPHOGENETIC_DISPERSION_MODULO = 1;
   public static int       MORPHOGENETIC_DISPERSION_MODULO         = DEFAULT_MORPHOGENETIC_DISPERSION_MODULO;

   // Cells.
   public Cell[][] cells;

   // Predecessor cells.
   public Cell[][] predecessorCells;

   // Metamorphs.
   public Vector<Metamorph> metamorphs;

   // Cells editable?
   public boolean isEditable = false;

   // Update ticks.
   public int tick;

   // Metamorph files.
   public String genFilename  = null;
   public String execFilename = null;

   // Metamorph data streams.
   public DataOutputStream writer;
   public DataInputStream  reader;

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

      predecessorCells = new Cell[DIMENSIONS.width][DIMENSIONS.height];
      metamorphs       = new Vector<Metamorph>();
      tick             = 0;
   }


   // Wrap x coordinate.
   public static int wrapX(int x)
   {
      int w = DIMENSIONS.width;

      while (x < 0) { x += w; }
      while (x >= w) { x -= w; }
      return(x);
   }


   // Wrap y coordinate.
   public static int wrapY(int y)
   {
      int h = DIMENSIONS.height;

      while (y < 0) { y += h; }
      while (y >= h) { y -= h; }
      return(y);
   }


   // Update.
   public void update()
   {
      initUpdate();
      tick++;
   }


   // Initialize update.
   public void initUpdate()
   {
      int x, y;

      // Generate morphogenetic fields.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            if ((cells[x][y].type != Cell.EMPTY) && morphogeneticLocale(x, y))
            {
               cells[x][y].generateMorphogen();
            }
            else
            {
               cells[x][y].morphogen = null;
            }
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
   }


   // Save parameters.
   public void saveParms(DataOutputStream writer) throws IOException
   {
      writer.writeInt(Cell.NUM_TYPES);
      writer.writeInt(Morphogen.NEIGHBORHOOD_DIMENSION);
      writer.writeInt(Morphogen.NUM_NEIGHBORHOODS);
      writer.writeInt(Organism.DIMENSIONS.width);
      writer.writeInt(Organism.DIMENSIONS.height);
      writer.writeInt(Organism.MORPHOGENETIC_DISPERSION_MODULO);
      writer.flush();
   }


   // Load parameters.
   public void loadParms(DataInputStream reader) throws IOException
   {
      int n = reader.readInt();

      if (n != Cell.NUM_TYPES)
      {
         throw new IOException("Cell numTypes (" + n + ") in file " + execFilename +
                               " must equal cell numTypes (" + Cell.NUM_TYPES + ")");
      }
      n = reader.readInt();
      if (n != Morphogen.NEIGHBORHOOD_DIMENSION)
      {
         throw new IOException("Morphogen neighborhoodDimension (" + n + ") in file " + execFilename +
                               " must equal neighborhoodDimension (" + Morphogen.NEIGHBORHOOD_DIMENSION + ")");
      }
      n = reader.readInt();
      if (n != Morphogen.NUM_NEIGHBORHOODS)
      {
         throw new IOException("Morphogen numNeighborhoods (" + n + ") in file " + execFilename +
                               " must equal numNeighborhoods (" + Morphogen.NUM_NEIGHBORHOODS + ")");
      }
      n = reader.readInt();
      if (n != Organism.DIMENSIONS.width)
      {
         throw new IOException("Organism dimensions width (" + n + ") in file " + execFilename +
                               " must equal dimensions width (" + Organism.DIMENSIONS.width + ")");
      }
      n = reader.readInt();
      if (n != Organism.DIMENSIONS.height)
      {
         throw new IOException("Organism dimensions height (" + n + ") in file " + execFilename +
                               " must equal dimensions height (" + Organism.DIMENSIONS.height + ")");
      }
      n = reader.readInt();
      if (n != Organism.MORPHOGENETIC_DISPERSION_MODULO)
      {
         throw new IOException("Organism morphogeneticDispersionModulo (" + n + ") in file " + execFilename +
                               " must equal morphogeneticDispersionModulo (" + Organism.MORPHOGENETIC_DISPERSION_MODULO + ")");
      }
   }


   // Save metamorphs.
   public void saveMetamorphs()
   {
      int x, y;

      try
      {
         for (x = 0; x < DIMENSIONS.width; x++)
         {
            for (y = 0; y < DIMENSIONS.height; y++)
            {
               if (predecessorCells[x][y].type != Cell.EMPTY)
               {
                  if (predecessorCells[x][y].morphogen != null)
                  {
                     saveMetamorph(predecessorCells[x][y].morphogen, cells[x][y]);
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


   // Save metamorph.
   public void saveMetamorph(Morphogen morphogen, Cell cell) throws IOException
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


   // Metamorph morphogen distance.
   public class MetamorphDistance
   {
      public float     morphogenDistance;
      public Metamorph metamorph;
      public boolean   mark;

      public MetamorphDistance(float d, Metamorph m)
      {
         morphogenDistance = d;
         metamorph         = m;
         mark = false;
      }
   }

   // Execute metamorphs.
   public void execMetamorphs()
   {
      int x, y, x2, y2;

      // Match metamorphs to cell morphogens.
      MetamorphDistance cellMorphs[][] = new MetamorphDistance[DIMENSIONS.width][DIMENSIONS.height];

      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            if ((predecessorCells[x][y].type != Cell.EMPTY) && morphogeneticLocale(x, y))
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

      // Morphs with better morphogen matches eliminate competing morphs.
      boolean active = true;
      while (active)
      {
         active = false;
         float     dist  = -1.0f;
         Metamorph morph = null;
         int       cx    = 0;
         int       cy    = 0;
         for (x = 0, x2 = randomizer.nextInt(DIMENSIONS.width); x < DIMENSIONS.width; x++)
         {
            for (y = 0, y2 = randomizer.nextInt(DIMENSIONS.height); y < DIMENSIONS.height; y++)
            {
               MetamorphDistance m = cellMorphs[x2][y2];
               if ((m != null) && !m.mark)
               {
                  if ((dist < 0.0f) || (m.morphogenDistance < dist))
                  {
                     dist  = m.morphogenDistance;
                     morph = m.metamorph;
                     cx    = x2;
                     cy    = y2;
                  }
               }
               y2 = (y2 + 1) % DIMENSIONS.height;
            }
            x2 = (x2 + 1) % DIMENSIONS.width;
         }
         if (morph != null)
         {
            Morphogen morphogen = morph.morphogen;
            for (x = 0; x < Morphogen.NEIGHBORHOOD_DIMENSION; x++)
            {
               for (y = 0; y < Morphogen.NEIGHBORHOOD_DIMENSION; y++)
               {
                  x2 = wrapX(cx + morphogen.sourceCells[x][y].x);
                  y2 = wrapY(cy + morphogen.sourceCells[x][y].y);
                  if ((x2 != cx) || (y2 != cy))
                  {
                     if ((morphogen.sourceCells[x][y].type != predecessorCells[x2][y2].type) ||
                         (morphogen.sourceCells[x][y].orientation != predecessorCells[x2][y2].orientation))
                     {
                        cellMorphs[x2][y2] = null;
                     }
                  }
               }
            }
            cellMorphs[cx][cy].mark = true;
            active = true;
         }
      }

      // Execute metamorphs from worse to better morphogen match.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            MetamorphDistance m = cellMorphs[x][y];
            if (m != null)
            {
               m.mark = false;
            }
         }
      }
      active = true;
      while (active)
      {
         active = false;
         float     dist  = -1.0f;
         Metamorph morph = null;
         int       cx    = 0;
         int       cy    = 0;
         for (x = 0, x2 = randomizer.nextInt(DIMENSIONS.width); x < DIMENSIONS.width; x++)
         {
            for (y = 0, y2 = randomizer.nextInt(DIMENSIONS.height); y < DIMENSIONS.height; y++)
            {
               MetamorphDistance m = cellMorphs[x2][y2];
               if ((m != null) && !m.mark)
               {
                  if ((dist < 0.0f) || (m.morphogenDistance > dist))
                  {
                     dist  = m.morphogenDistance;
                     morph = m.metamorph;
                     cx    = x2;
                     cy    = y2;
                  }
               }
               y2 = (y2 + 1) % DIMENSIONS.height;
            }
            x2 = (x2 + 1) % DIMENSIONS.width;
         }
         if (morph != null)
         {
            morph.exec(cells[cx][cy]);
            cellMorphs[cx][cy].mark = true;
            active = true;
         }
      }
   }


   // Is a morphogenetic field centered at this locale?
   public boolean morphogeneticLocale(int x, int y)
   {
      if (((x % MORPHOGENETIC_DISPERSION_MODULO) == 0) &&
          ((y % MORPHOGENETIC_DISPERSION_MODULO) == 0))
      {
         return(true);
      }
      else
      {
         return(false);
      }
   }
}
