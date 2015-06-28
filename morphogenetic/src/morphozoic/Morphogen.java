// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;

/*
 * Morphogenetic field:
 * A field is a set of nested spheres of increasing size.
 * Each sphere spans a central sector of cells and the sectors in its Moore neighborhood.
 * A vector of cell type densities is associated with each sector.
 */
public class Morphogen
{
   // Spheres.
   public static final int DEFAULT_NUM_SPHERES = 3;
   public static int       NUM_SPHERES         = DEFAULT_NUM_SPHERES;
   public Vector<Sphere>   spheres;

   // Sphere sector dimension: odd number.
   public static final int DEFAULT_SECTOR_DIMENSION = 3;
   public static int       SECTOR_DIMENSION         = DEFAULT_SECTOR_DIMENSION;

   // Sphere.
   public class Sphere
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
            typeDensities = new float[Cell.NUM_TYPES];
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

      public Sphere()
      {
         sectors = new Sector[SECTOR_DIMENSION * SECTOR_DIMENSION];
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

   // Hash code.
   public int hashCode;

   // Constructors.
   public Morphogen(Cell cell)
   {
      // Create spheres.
      spheres = new Vector<Sphere>();
      for (int i = 0; i < NUM_SPHERES; i++)
      {
         spheres.add(generateSphere(cell, i));
      }

      // Create hash code.
      hashCode = getHashCode();
   }


   public Morphogen()
   {
      spheres  = new Vector<Sphere>();
      hashCode = 0;
   }


   // Generate field sphere.
   private Sphere generateSphere(Cell cell, int sphereNum)
   {
      Sphere sphere = new Sphere();

      Cell[][] cells = cell.organism.cells;
      int   w  = cell.organism.DIMENSIONS.width;
      int   h  = cell.organism.DIMENSIONS.height;
      int   d  = (int)Math.pow((double)SECTOR_DIMENSION, (double)sphereNum);
      float d2 = (float)(d * d);
      int   o  = (d * SECTOR_DIMENSION) / 2;
      int   x  = cell.x - o;
      int   y  = cell.y - o;
      for (int y1 = 0, b = 0; y1 < SECTOR_DIMENSION; y1++)
      {
         for (int x1 = 0; x1 < SECTOR_DIMENSION; x1++)
         {
            int x2  = x + (x1 * d);
            int y2  = y + (y1 * d);
            int t[] = new int[Cell.NUM_TYPES];
            for (int y3 = 0; y3 < d; y3++)
            {
               for (int x3 = 0; x3 < d; x3++)
               {
                  int x4 = x2 + x3;
                  while (x4 < 0) { x4 += w; }
                  while (x4 >= w) { x4 -= w; }
                  int y4 = y2 + y3;
                  while (y4 < 0) { y4 += h; }
                  while (y4 >= h) { y4 -= h; }
                  if (cells[x4][y4].type != Cell.EMPTY)
                  {
                     t[cells[x4][y4].type]++;
                  }
               }
            }
            Sphere.Sector sector = sphere.addSector(b++, x2 - cell.x, y2 - cell.y, d);
            for (int i = 0; i < Cell.NUM_TYPES; i++)
            {
               sector.setTypeDensity(i, (float)t[i] / d2);
            }
         }
      }
      return(sphere);
   }


   // Get hash code.
   public int getHashCode()
   {
      Random r = new Random(65);

      for (int i = 0; i < NUM_SPHERES; i++)
      {
         Sphere sphere = spheres.get(i);
         for (int j = 0; j < sphere.sectors.length; j++)
         {
            Sphere.Sector sector = sphere.getSector(j);
            for (int k = 0; k < Cell.NUM_TYPES; k++)
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


   // Get a sphere.
   public Sphere getSphere(int sphereNum)
   {
      return(spheres.get(sphereNum));
   }


   // Compare.
   public float compare(Morphogen morphogen)
   {
      float delta = 0.0f;

      if (morphogen.hashCode == hashCode)
      {
         return(0.0f);
      }
      for (int i = 0; i < NUM_SPHERES; i++)
      {
         Sphere s1 = getSphere(i);
         Sphere s2 = morphogen.getSphere(i);
         for (int j = 0; j < s1.sectors.length; j++)
         {
            Sphere.Sector t1 = s1.sectors[j];
            Sphere.Sector t2 = s2.sectors[j];
            for (int k = 0; k < t1.typeDensities.length; k++)
            {
               delta += Math.abs(t1.typeDensities[k] - t2.typeDensities[k]);
            }
         }
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
      for (Sphere s : spheres)
      {
         for (int i = 0; i < s.sectors.length; i++)
         {
            Sphere.Sector t = s.sectors[i];
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

      for (int i = 0; i < NUM_SPHERES; i++)
      {
         Sphere s = m.new Sphere();
         m.spheres.add(s);
         for (int j = 0; j < s.sectors.length; j++)
         {
            int           dx = reader.readInt();
            int           dy = reader.readInt();
            int           d  = reader.readInt();
            Sphere.Sector t  = s.new Sector(dx, dy, d);
            for (int k = 0; k < Cell.NUM_TYPES; k++)
            {
               t.setTypeDensity(k, reader.readFloat());
            }
            s.sectors[j] = t;
         }
      }
      m.hashCode = reader.readInt();
      return(m);
   }


   // Print.
   public void print()
   {
      System.out.println("Morphogen:");
      printNeighborhood();
      System.out.println("  Spheres:");
      for (int s = 0; s < spheres.size(); s++)
      {
         System.out.println("    Sphere " + s + ":");
         for (int i = 0; i < spheres.get(s).sectors.length; i++)
         {
            System.out.print("      Sector " + i + ":");
            Sphere.Sector t = spheres.get(s).sectors[i];
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


   // Print neighborhood cells.
   public void printNeighborhood()
   {
      System.out.println("  Neighborhood cells:");
      int    types[][] = new int[SECTOR_DIMENSION][SECTOR_DIMENSION];
      Sphere sphere    = spheres.get(0);
      for (int i = 0, x = 0, y = 0; i < sphere.sectors.length; i++)
      {
         Sphere.Sector t = sphere.sectors[i];
         types[x][y] = Cell.EMPTY;
         for (int j = 0; j < t.typeDensities.length; j++)
         {
            if (t.typeDensities[j] > 0.0f)
            {
               types[x][y] = j;
               break;
            }
         }
         x++;
         if (x == SECTOR_DIMENSION)
         {
            x = 0;
            y++;
         }
      }
      for (int y = SECTOR_DIMENSION - 1; y >= 0; y--)
      {
         for (int x = 0; x < SECTOR_DIMENSION; x++)
         {
            if (types[x][y] == Cell.EMPTY)
            {
               System.out.print("\tx");
            }
            else
            {
               System.out.print("\t" + types[x][y]);
            }
         }
         System.out.println();
      }
   }
}
