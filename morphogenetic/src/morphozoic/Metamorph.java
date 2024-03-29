// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import rdtree.RDclient;

// Metamorph.
public class Metamorph implements RDclient
{
   // Morphogen.
   public Morphogen morphogen;

   // Target cell configuration.
   public Cell[][] targetCells;

   // Hash code.
   public int hashCode;

   // Usage flag.
   public boolean usage;

   // Constructors.
   public Metamorph(Morphogen morphogen, Cell cell)
   {
      this.morphogen = morphogen;
      targetCells    = new Cell[Parameters.METAMORPH_DIMENSION][Parameters.METAMORPH_DIMENSION];
      Cell[][] cells = cell.organism.cells;
      int o  = Parameters.METAMORPH_DIMENSION / 2;
      int cx = cell.x - o;
      int cy = cell.y - o;
      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            int x2 = Organism.wrapX(cx + x);
            int y2 = Organism.wrapY(cy + y);
            targetCells[x][y]   = cells[x2][y2].clone();
            targetCells[x][y].x = x - o;
            targetCells[x][y].y = y - o;
         }
      }
      hashCode = getHashCode();
      usage    = false;
   }


   public Metamorph(Morphogen morphogen, Cell[][] targetCells)
   {
      this.morphogen   = morphogen;
      this.targetCells = targetCells;
      hashCode         = getHashCode();
      usage            = false;
   }


   // Get hash code.
   public int getHashCode()
   {
      Random r = new Random(66);

      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            int h = r.nextInt();
            int t = targetCells[x][y].type;
            if (t != 0)
            {
               h = h ^ t;
               r.setSeed(h);
            }
            h = r.nextInt();
            int o = targetCells[x][y].orientation.ordinal();
            if (o != 0)
            {
               h = h ^ o;
               r.setSeed(h);
            }
         }
      }
      return(r.nextInt());
   }


   // Equality test.
   public boolean equals(Metamorph m)
   {
      if (m.morphogen.hashCode != morphogen.hashCode)
      {
         return(false);
      }
      if (m.hashCode != hashCode)
      {
         // Conflicting metamorphs!
         return(false);
      }
      else
      {
         return(true);
      }
   }


   // RDclient distance.
   public float dist(RDclient client)
   {
      return(morphogen.compare(((Metamorph)client).morphogen));
   }


   // Execute: overlay cell neighborhood with target cells.
   public void exec(Cell cell)
   {
      Cell[][] cells = cell.organism.cells;
      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            int x2 = Organism.wrapX(cell.x + targetCells[x][y].x);
            int y2 = Organism.wrapY(cell.y + targetCells[x][y].y);
            cells[x2][y2].type        = targetCells[x][y].type;
            cells[x2][y2].orientation = targetCells[x][y].orientation;
         }
      }
   }


   // Execution helper: Cell properties metamorph.
   public class CellProps implements Comparable<CellProps>
   {
      public int         type;
      public Orientation orientation;
      public Float       morphogenDistance;

      public CellProps(int t, Orientation o, float d)
      {
         type              = t;
         orientation       = o;
         morphogenDistance = d;
      }


      // For descending order sort by morphogen distance.
      public int compareTo(CellProps other)
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

   // Execution helper: Cell properties metamorph list.
   public class CellPropsList
   {
      public ArrayList<CellProps> morphs;

      public CellPropsList()
      {
         morphs = new ArrayList<CellProps>();
      }


      public void add(int t, Orientation o, float d)
      {
         morphs.add(new CellProps(t, o, d));
         Collections.sort(morphs);
      }


      public void clear()
      {
         morphs.clear();
      }
   }

   // Add cell morph properties.
   public void addCellProps(CellPropsList[][] cellPropsLists, int cx, int cy, float dist)
   {
      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            int x2 = Organism.wrapX(cx + targetCells[x][y].x);
            int y2 = Organism.wrapY(cy + targetCells[x][y].y);
            if (cellPropsLists[x2][y2] == null)
            {
               cellPropsLists[x2][y2] = new CellPropsList();
            }
            cellPropsLists[x2][y2].add(targetCells[x][y].type, targetCells[x][y].orientation, dist);
         }
      }
   }


   // Execute cell properties morph.
   public static void exec(Cell cell, CellPropsList[][] cellPropsLists, Random randomizer)
   {
      int x = cell.x;
      int y = cell.y;

      ArrayList<CellProps> morphs = cellPropsLists[x][y].morphs;
      int n = morphs.size();
      float[] weights = new float[n];
      float sum = 0.0f;
      Collections.reverse(morphs);
      if (Parameters.PROBABILISTIC_METAMORPH)
      {
         for (int i = 0; i < n; i++)
         {
            CellProps m = morphs.get(i);
            weights[i] = m.morphogenDistance + Parameters.METAMORPH_RANDOM_BIAS;
            sum       += weights[i];
         }
         if (sum > 0.0f)
         {
            for (int i = 0; i < n; i++)
            {
               weights[i] = (sum - weights[i]) / sum;
            }
            CellProps m = morphs.get(0);
            for (int i = 0; i < n; i++)
            {
               if (randomizer.nextFloat() < weights[i]) { break; }
               morphs.remove(0);
            }
            if (morphs.size() == 0)
            {
               morphs.add(m);
            }
         }
      }
      cell.type        = morphs.get(0).type;
      cell.orientation = morphs.get(0).orientation;
   }


   // Save.
   public void save(DataOutputStream writer) throws IOException
   {
      morphogen.save(writer);
      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            writer.writeInt(targetCells[x][y].type);
            writer.writeInt(targetCells[x][y].orientation.ordinal());
         }
      }
      writer.flush();
   }


   // Load.
   public static Metamorph load(DataInputStream reader) throws IOException
   {
      try
      {
         Morphogen morphogen = Morphogen.load(reader);
         Cell[][] targetCells = new Cell[Parameters.METAMORPH_DIMENSION][Parameters.METAMORPH_DIMENSION];
         int d = Parameters.METAMORPH_DIMENSION / 2;
         for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
         {
            for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
            {
               int t = reader.readInt();
               int o = reader.readInt();
               targetCells[x][y] = new Cell(t, x - d, y - d, Orientation.fromInt(o), null);
            }
         }
         return(new Metamorph(morphogen, targetCells));
      }
      catch (EOFException e) {
         return(null);
      }
   }


   // Clone.
   public Metamorph clone()
   {
      return(new Metamorph(morphogen.clone(), cloneTargetCells()));
   }


   // Clone target cells.
   public Cell[][] cloneTargetCells()
   {
      Cell[][] targetCells = new Cell[Parameters.METAMORPH_DIMENSION][Parameters.METAMORPH_DIMENSION];
      for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.METAMORPH_DIMENSION; y++)
         {
            targetCells[x][y] = this.targetCells[x][y].clone();
         }
      }
      return(targetCells);
   }


   // Mutate.
   public Metamorph mutate(Random randomizer, float maxCellTypeDensity)
   {
      // Clone metamorph.
      Metamorph metamorph = clone();

      // Mutate.
      int r = randomizer.nextInt(metamorph.morphogen.neighborhoods.size() + 1);

      if (r == 0)
      {
         // Mutate random target cell type.
         int t = randomizer.nextInt(Parameters.NUM_CELL_TYPES + 1);
         int x = randomizer.nextInt(Parameters.METAMORPH_DIMENSION);
         int y = randomizer.nextInt(Parameters.METAMORPH_DIMENSION);
         if (t == 0)
         {
            metamorph.targetCells[x][y].type = Cell.EMPTY;
         }
         else
         {
            metamorph.targetCells[x][y].type = t - 1;
         }
      }
      else
      {
         // Mutate random neighborhood sector type density.
         int n = r - 1;
         int s = randomizer.nextInt(metamorph.morphogen.neighborhoods.get(n).sectors.length);
         int t = randomizer.nextInt(Parameters.NUM_CELL_TYPES);
         if (n == 0)
         {
            for (int i = 0; i < Parameters.NUM_CELL_TYPES; i++)
            {
               metamorph.morphogen.neighborhoods.get(n).sectors[s].typeDensities[i] = 0.0f;
            }
            metamorph.morphogen.neighborhoods.get(n).sectors[s].typeDensities[t] = 1.0f;
         }
         else
         {
            metamorph.morphogen.neighborhoods.get(n).sectors[s].typeDensities[t] = randomizer.nextFloat() * maxCellTypeDensity;
         }
      }
      return(metamorph);
   }


   // Print.
   public void print()
   {
      System.out.println("Metamorph:");
      morphogen.print();
      System.out.println("  Target cells:");
      for (int y = Parameters.METAMORPH_DIMENSION - 1; y >= 0; y--)
      {
         for (int x = 0; x < Parameters.METAMORPH_DIMENSION; x++)
         {
            if (targetCells[x][y].type == Cell.EMPTY)
            {
               System.out.print("\tx");
            }
            else
            {
               System.out.print("\t" + targetCells[x][y].type);
            }
         }
         System.out.println();
      }
   }
}
