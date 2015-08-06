// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;

import rdtree.RDclient;
import rdtree.RDtree;
import rdtree.RDtree.RDsearch;

// Organism.
public class Organism
{
   // Random numbers.
   public Random randomizer;

   // Cells.
   public Cell[][] cells;

   // Predecessor cells.
   public Cell[][] predecessorCells;

   // Metamorphs.
   public Vector<Metamorph> metamorphs;

   // Metamorph search tree.
   public RDtree metamorphSearch;

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
   public Organism(String[] args, Integer id)
   {
      init();
   }


   public Organism()
   {
      init();
   }


   // Initialize.
   private void init()
   {
      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Create cells.
      cells = new Cell[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y] = new Cell(Cell.EMPTY, x, y, Orientation.NORTH, this);
         }
      }

      predecessorCells = new Cell[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      metamorphs       = new Vector<Metamorph>();
      metamorphSearch  = new RDtree();
      tick             = 0;
   }


   // Wrap x coordinate.
   public static int wrapX(int x)
   {
      int w = Parameters.ORGANISM_DIMENSIONS.width;

      while (x < 0) { x += w; }
      while (x >= w) { x -= w; }
      return(x);
   }


   // Wrap y coordinate.
   public static int wrapY(int y)
   {
      int h = Parameters.ORGANISM_DIMENSIONS.height;

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
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if ((cells[x][y].type != Cell.EMPTY) && morphogeneticCell(x, y))
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
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            predecessorCells[x][y]           = cells[x][y].clone();
            predecessorCells[x][y].morphogen = cells[x][y].morphogen;
            cells[x][y].morphogen            = null;
         }
      }
   }


   // Save metamorphs.
   public void saveMetamorphs()
   {
      int x, y;

      try
      {
         for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
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
      writer.flush();
   }


   // Execution helper: Metamorph morphogen distance.
   public class MetamorphDistance implements Comparable<MetamorphDistance>
   {
      public Metamorph metamorph;
      public Float     morphogenDistance;

      public MetamorphDistance(Metamorph m, float d)
      {
         metamorph         = m;
         morphogenDistance = d;
      }


      // For descending order sort by morphogen distance.
      public int compareTo(MetamorphDistance other)
      {
         float d = other.morphogenDistance - morphogenDistance;

         if (d < 0.0f)
         {
            return(-1);
         }
         else if (d > 0.0f)
         {
            return(1);
         }
         else
         {
            return(0);
         }
      }
   }

   // Execution helper: Cell metamorphs.
   public class CellMetamorphs
   {
      public ArrayList<MetamorphDistance> morphs;
      public boolean mark;

      public CellMetamorphs()
      {
         morphs = new ArrayList<MetamorphDistance>();
         mark   = false;
      }


      public void add(Metamorph m, float d)
      {
         morphs.add(new MetamorphDistance(m, d));
         Collections.sort(morphs);
         while (morphs.size() > Parameters.MAX_CELL_METAMORPHS)
         {
            morphs.remove(0);
         }
      }


      public void clear()
      {
         morphs.clear();
      }
   }

   // Execute metamorphs.
   public void execMetamorphs()
   {
      int x, y, x2, y2, n;

      // Match metamorphs to cell morphogens.
      CellMetamorphs[][] cellMorphs =
         new CellMetamorphs[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];

      if (((n = metamorphs.size()) > 0) && (Parameters.MAX_CELL_METAMORPHS > 0))
      {
         for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               if ((predecessorCells[x][y].type != Cell.EMPTY) && morphogeneticCell(x, y))
               {
                  if (Parameters.EXEC_METAMORPHS_WITH_SEARCH_TREE)
                  {
                     Metamorph m          = new Metamorph(predecessorCells[x][y].morphogen, cells[x][y]);
                     RDsearch  searchList = metamorphSearch.search((RDclient)m, Parameters.MAX_CELL_METAMORPHS, n);
                     for ( ; searchList != null; searchList = searchList.srchnext)
                     {
                        float d = searchList.distance;
                        if (d <= Parameters.MAX_MORPHOGEN_COMPARE_DISTANCE)
                        {
                           m = (Metamorph)searchList.node.client;
                           if (cellMorphs[x][y] == null)
                           {
                              cellMorphs[x][y] = new CellMetamorphs();
                           }
                           cellMorphs[x][y].add(m, d);
                        }
                     }
                  }
                  else
                  {
                     for (int i = 0, j = randomizer.nextInt(n); i < n; i++, j = (j + 1) % n)
                     {
                        Metamorph m = metamorphs.get(j);
                        float     d = predecessorCells[x][y].morphogen.compare(m.morphogen);
                        if (d <= Parameters.MAX_MORPHOGEN_COMPARE_DISTANCE)
                        {
                           if (cellMorphs[x][y] == null)
                           {
                              cellMorphs[x][y] = new CellMetamorphs();
                           }
                           cellMorphs[x][y].add(m, d);
                        }
                     }
                  }
               }
            }
         }
      }

      // Probabilistically choose neighborhood matching morphogen,
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if (cellMorphs[x][y] != null)
            {
               chooseMetamorph(cellMorphs[x][y].morphs);
            }
         }
      }

      // Morphs with better morphogen matches inhibit competing morphs?
      boolean active = true;
      if (Parameters.INHIBIT_COMPETING_MORPHOGENS)
      {
         while (active)
         {
            active = false;
            float     dist  = -1.0f;
            Metamorph morph = null;
            int       cx    = 0;
            int       cy    = 0;
            for (x = 0, x2 = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.width);
                 x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0, y2 = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.height);
                    y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  CellMetamorphs m = cellMorphs[x2][y2];
                  if ((m != null) && !m.mark)
                  {
                     float d = m.morphs.get(0).morphogenDistance;
                     if ((dist < 0.0f) || (d < dist))
                     {
                        dist  = d;
                        morph = m.morphs.get(0).metamorph;
                        cx    = x2;
                        cy    = y2;
                     }
                  }
                  y2 = (y2 + 1) % Parameters.ORGANISM_DIMENSIONS.height;
               }
               x2 = (x2 + 1) % Parameters.ORGANISM_DIMENSIONS.width;
            }
            if (morph != null)
            {
               Morphogen morphogen = morph.morphogen;
               for (x = 0; x < Parameters.NEIGHBORHOOD_DIMENSION; x++)
               {
                  for (y = 0; y < Parameters.NEIGHBORHOOD_DIMENSION; y++)
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
      }

      // Execute metamorphs.
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            CellMetamorphs m = cellMorphs[x][y];
            if (m != null)
            {
               m.mark = false;
            }
         }
      }
      Metamorph.CellPropsList[][] cellPropsMorphs =
         new Metamorph.CellPropsList[Parameters.ORGANISM_DIMENSIONS.width][Parameters.ORGANISM_DIMENSIONS.height];
      active = true;
      while (active)
      {
         active = false;
         float     dist  = -1.0f;
         Metamorph morph = null;
         int       cx    = 0;
         int       cy    = 0;
         for (x = 0, x2 = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.width);
              x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (y = 0, y2 = randomizer.nextInt(Parameters.ORGANISM_DIMENSIONS.height);
                 y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               CellMetamorphs m = cellMorphs[x2][y2];
               if ((m != null) && !m.mark)
               {
                  float d = m.morphs.get(0).morphogenDistance;
                  if ((dist < 0.0f) || (d > dist))
                  {
                     dist  = d;
                     morph = m.morphs.get(0).metamorph;
                     cx    = x2;
                     cy    = y2;
                  }
               }
               y2 = (y2 + 1) % Parameters.ORGANISM_DIMENSIONS.height;
            }
            x2 = (x2 + 1) % Parameters.ORGANISM_DIMENSIONS.width;
         }
         if (morph != null)
         {
            morph.addCellProps(cellPropsMorphs, cx, cy, dist);
            cellMorphs[cx][cy].mark = true;
            active = true;
         }
      }
      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if (cellPropsMorphs[x][y] != null)
            {
               Metamorph.exec(cells[x][y], cellPropsMorphs, randomizer);
            }
         }
      }
   }


   // Weighted choice of metamorph by morphogen distance.
   private void chooseMetamorph(ArrayList<MetamorphDistance> metamorphs)
   {
      int n = metamorphs.size();

      float[] weights = new float[n];
      float sum = 0.0f;
      Collections.reverse(metamorphs);
      for (int i = 0; i < n; i++)
      {
         MetamorphDistance m = metamorphs.get(i);
         weights[i] = m.morphogenDistance + Parameters.METAMORPH_RANDOM_BIAS;
         sum       += weights[i];
      }
      if (sum > 0.0f)
      {
         for (int i = 0; i < n; i++)
         {
            weights[i] = (sum - weights[i]) / sum;
         }
         MetamorphDistance m = metamorphs.get(0);
         for (int i = 0; i < n; i++)
         {
            if (randomizer.nextFloat() < weights[i]) { break; }
            metamorphs.remove(0);
         }
         if (metamorphs.size() == 0)
         {
            metamorphs.add(m);
         }
      }
   }


   // Is a morphogenetic field at this cell location?
   public boolean morphogeneticCell(int x, int y)
   {
      if (((x % Parameters.MORPHOGENETIC_CELL_DISPERSION_MODULO) == 0) &&
          ((y % Parameters.MORPHOGENETIC_CELL_DISPERSION_MODULO) == 0))
      {
         return(true);
      }
      else
      {
         return(false);
      }
   }


   // Get color for cell type.
   public Color getColor(int type)
   {
      return(Cell.getColor(type));
   }
}
