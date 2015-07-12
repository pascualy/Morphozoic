// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;

/*
 * Morphogenetic field:
 * A field is a set of nested neighborhoods of increasing size.
 * A neighborhood is an NxN configuration sub-neighborhood sectors.
 * The smallest neighborhood is a single cell.
 * A sector contains a vector of cell type densities from its neighborhood.
 */
public class Morphogen
{
   // Source cell configuration.
   public Cell[][] sourceCells;

   // Neighborhood.
   public class Neighborhood
   {
      // Sector.
      public class Sector
      {
         public float[] typeDensities;
         public int     dx, dy, d;

         public Sector(int dx, int dy, int d)
         {
            this.dx       = dx;
            this.dy       = dy;
            this.d        = d;
            typeDensities = new float[Parameters.NUM_CELL_TYPES];
         }


         public void setTypeDensity(int index, float density)
         {
            typeDensities[index] = density;
         }


         public float getTypeDensity(int index)
         {
            return(typeDensities[index]);
         }
      }

      public Sector[] sectors;

      public Neighborhood()
      {
         sectors = new Sector[Parameters.NEIGHBORHOOD_DIMENSION * Parameters.NEIGHBORHOOD_DIMENSION];
      }


      public Sector addSector(int index, int dx, int dy, int d)
      {
         Sector sector = new Sector(dx, dy, d);

         sectors[index] = sector;
         return(sector);
      }


      public Sector getSector(int index)
      {
         return(sectors[index]);
      }
   }

   // Neighborhoods.
   public Vector<Neighborhood> neighborhoods;

   // Hash code.
   public int hashCode;

   // Constructors.
   public Morphogen(Cell cell)
   {
      // Create source cell configuration.
      sourceCells    = new Cell[Parameters.NEIGHBORHOOD_DIMENSION][Parameters.NEIGHBORHOOD_DIMENSION];
      Cell[][] cells = cell.organism.cells;
      int o  = Parameters.NEIGHBORHOOD_DIMENSION / 2;
      int cx = cell.x - o;
      int cy = cell.y - o;
      for (int x = 0; x < Parameters.NEIGHBORHOOD_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.NEIGHBORHOOD_DIMENSION; y++)
         {
            int x2 = Organism.wrapX(cx + x);
            int y2 = Organism.wrapY(cy + y);
            sourceCells[x][y]   = cells[x2][y2].clone();
            sourceCells[x][y].x = x - o;
            sourceCells[x][y].y = y - o;
         }
      }

      // Create neighborhoods.
      neighborhoods = new Vector<Neighborhood>();
      for (int i = 0; i < Parameters.NUM_NEIGHBORHOODS; i++)
      {
         neighborhoods.add(generateNeighborhood(cell, i));
      }

      // Create hash code.
      hashCode = getHashCode();
   }


   public Morphogen()
   {
      sourceCells   = null;
      neighborhoods = null;
      hashCode      = 0;
   }


   // Generate field neighborhood.
   private Neighborhood generateNeighborhood(Cell cell, int neighborhoodNum)
   {
      Neighborhood neighborhood = new Neighborhood();

      Cell[][] cells = cell.organism.cells;
      int   d  = (int)Math.pow((double)Parameters.NEIGHBORHOOD_DIMENSION, (double)neighborhoodNum);
      float d2 = (float)(d * d);
      int   o  = (d * Parameters.NEIGHBORHOOD_DIMENSION) / 2;
      int   x  = cell.x - o;
      int   y  = cell.y - o;
      for (int y1 = 0, b = 0; y1 < Parameters.NEIGHBORHOOD_DIMENSION; y1++)
      {
         for (int x1 = 0; x1 < Parameters.NEIGHBORHOOD_DIMENSION; x1++)
         {
            int x2  = x + (x1 * d);
            int y2  = y + (y1 * d);
            int t[] = new int[Parameters.NUM_CELL_TYPES];
            for (int y3 = 0; y3 < d; y3++)
            {
               for (int x3 = 0; x3 < d; x3++)
               {
                  int x4 = Organism.wrapX(x2 + x3);
                  int y4 = Organism.wrapY(y2 + y3);
                  if (cells[x4][y4].type != Cell.EMPTY)
                  {
                     t[cells[x4][y4].type]++;
                  }
               }
            }
            Neighborhood.Sector sector = neighborhood.addSector(b++, x2 - cell.x, y2 - cell.y, d);
            for (int i = 0; i < Parameters.NUM_CELL_TYPES; i++)
            {
               sector.setTypeDensity(i, (float)t[i] / d2);
            }
         }
      }
      return(neighborhood);
   }


   // Get hash code.
   public int getHashCode()
   {
      Random r = new Random(65);

      for (int i = 0; i < Parameters.NUM_NEIGHBORHOODS; i++)
      {
         Neighborhood neighborhood = neighborhoods.get(i);
         for (int j = 0; j < neighborhood.sectors.length; j++)
         {
            Neighborhood.Sector sector = neighborhood.getSector(j);
            for (int k = 0; k < Parameters.NUM_CELL_TYPES; k++)
            {
               int   h = r.nextInt();
               float d = sector.getTypeDensity(k);
               if (d > 0.0f)
               {
                  h = h ^ Float.floatToIntBits(d);
                  r.setSeed(h);
               }
            }
         }
      }
      return(r.nextInt());
   }


   // Get a neighborhood.
   public Neighborhood getNeighborhood(int neighborhoodNum)
   {
      return(neighborhoods.get(neighborhoodNum));
   }


   // Compare.
   public float compare(Morphogen morphogen)
   {
      float delta = 0.0f;

      if (morphogen.hashCode == hashCode)
      {
         return(0.0f);
      }
      for (int i = 0; i < Parameters.NUM_NEIGHBORHOODS; i++)
      {
         Neighborhood n1     = getNeighborhood(i);
         Neighborhood n2     = morphogen.getNeighborhood(i);
         float        ndelta = 0.0f;
         for (int j = 0; j < n1.sectors.length; j++)
         {
            Neighborhood.Sector t1 = n1.sectors[j];
            Neighborhood.Sector t2 = n2.sectors[j];
            for (int k = 0; k < t1.typeDensities.length; k++)
            {
               ndelta += Math.abs(t1.typeDensities[k] - t2.typeDensities[k]);
            }
         }
         if (Parameters.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS != null)
         {
            ndelta *= Parameters.NESTED_NEIGHBORHOOD_IMPORTANCE_WEIGHTS[i];
         }
         else
         {
            ndelta /= (float)Parameters.NUM_NEIGHBORHOODS;
         }
         delta += ndelta;
      }
      return(delta);
   }


   // Equality test.
   public boolean equals(Morphogen morphogen)
   {
      if (compare(morphogen) == 0.0f)
      {
         return(true);
      }
      else
      {
         return(false);
      }
   }


   // Save.
   public void save(DataOutputStream writer) throws IOException
   {
      for (int x = 0; x < Parameters.NEIGHBORHOOD_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.NEIGHBORHOOD_DIMENSION; y++)
         {
            writer.writeInt(sourceCells[x][y].type);
            writer.writeInt(sourceCells[x][y].orientation.ordinal());
         }
      }
      for (Neighborhood n : neighborhoods)
      {
         for (int i = 0; i < n.sectors.length; i++)
         {
            Neighborhood.Sector t = n.sectors[i];
            writer.writeInt(t.dx);
            writer.writeInt(t.dy);
            writer.writeInt(t.d);
            for (int j = 0; j < t.typeDensities.length; j++)
            {
               writer.writeFloat(t.typeDensities[j]);
            }
         }
      }
      writer.writeInt(hashCode);
      writer.flush();
   }


   // Load.
   public static Morphogen load(DataInputStream reader) throws EOFException, IOException
   {
      Morphogen m = new Morphogen();

      m.sourceCells = new Cell[Parameters.NEIGHBORHOOD_DIMENSION][Parameters.NEIGHBORHOOD_DIMENSION];
      int d = Parameters.NEIGHBORHOOD_DIMENSION / 2;
      for (int x = 0; x < Parameters.NEIGHBORHOOD_DIMENSION; x++)
      {
         for (int y = 0; y < Parameters.NEIGHBORHOOD_DIMENSION; y++)
         {
            int t = reader.readInt();
            int o = reader.readInt();
            m.sourceCells[x][y] = new Cell(t, x - d, y - d, Orientation.fromInt(o), null);
         }
      }
      m.neighborhoods = new Vector<Neighborhood>();
      for (int i = 0; i < Parameters.NUM_NEIGHBORHOODS; i++)
      {
         Neighborhood n = m.new Neighborhood();
         m.neighborhoods.add(n);
         for (int j = 0; j < n.sectors.length; j++)
         {
            int dx = reader.readInt();
            int dy = reader.readInt();
            d = reader.readInt();
            Neighborhood.Sector t = n.new Sector(dx, dy, d);
            for (int k = 0; k < Parameters.NUM_CELL_TYPES; k++)
            {
               t.setTypeDensity(k, reader.readFloat());
            }
            n.sectors[j] = t;
         }
      }
      m.hashCode = reader.readInt();
      return(m);
   }


   // Print.
   public void print()
   {
      System.out.println("Morphogen:");
      System.out.println("  Source cells:");
      for (int y = Parameters.NEIGHBORHOOD_DIMENSION - 1; y >= 0; y--)
      {
         for (int x = 0; x < Parameters.NEIGHBORHOOD_DIMENSION; x++)
         {
            if (sourceCells[x][y].type == Cell.EMPTY)
            {
               System.out.print("\tx");
            }
            else
            {
               System.out.print("\t" + sourceCells[x][y].type);
            }
         }
         System.out.println();
      }
      System.out.println("  Neighborhoods:");
      for (int n = 0; n < neighborhoods.size(); n++)
      {
         System.out.println("    Neighborhood " + n + ":");
         for (int i = 0; i < neighborhoods.get(n).sectors.length; i++)
         {
            System.out.print("      Sector " + i + ":");
            Neighborhood.Sector t = neighborhoods.get(n).sectors[i];
            System.out.print(" dx=" + t.dx);
            System.out.print(" dy=" + t.dy);
            System.out.println(" d=" + t.d);
            System.out.print("        Type densities");
            for (int j = 0; j < t.typeDensities.length; j++)
            {
               System.out.print(" " + t.typeDensities[j]);
            }
            System.out.println();
         }
      }
      System.out.println("  Hash code=" + hashCode);
   }
}
