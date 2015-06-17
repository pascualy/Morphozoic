// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;

/*
 * Morphogenetic field:
 * A field is a set of enveloping spheres of increasing size.
 * Each sphere spans a central sector of cells and the sectors in its Moore neighborhood.
 * A vector of cell type densities is associated with each sector.
 */
public class Morphogen
{
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
            typeDensities = new float[Cell.numTypes];
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
         sectors = new Sector[9];
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

   public static final int SPHERES = 3;
   public Vector<Sphere>   spheres;

   // Constructors.
   public Morphogen(Cell cell)
   {
      spheres = new Vector<Sphere>();
      for (int i = 0; i < SPHERES; i++)
      {
         spheres.add(generateSphere(cell, i));
      }
   }


   public Morphogen()
   {
      spheres = new Vector<Sphere>();
   }


   // Generate field sphere.
   private Sphere generateSphere(Cell cell, int sphereNum)
   {
      Sphere sphere = new Sphere();

      Cell[][] cells = cell.organism.cells;
      int   w  = cell.organism.DIMENSIONS.width;
      int   h  = cell.organism.DIMENSIONS.height;
      int   d  = (int)Math.pow(3.0, (double)sphereNum);
      float d2 = (float)(d * d);
      int   o  = (d * 3) / 2;
      int   x  = cell.x - o;
      int   y  = cell.y - o;
      for (int y1 = 0, b = 0; y1 < 3; y1++)
      {
         for (int x1 = 0; x1 < 3; x1++)
         {
            int x2  = x + (x1 * d);
            int y2  = y + (y1 * d);
            int t[] = new int[Cell.numTypes];
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
            for (int i = 0; i < Cell.numTypes; i++)
            {
               sector.setTypeDensity(i, (float)t[i] / d2);
            }
         }
      }
      return(sphere);
   }


   // Get a sphere.
   public Sphere getSphere(int sphereNum)
   {
      return(spheres.get(sphereNum));
   }


   // Equality test.
   public boolean equals(Morphogen morphogen)
   {
      for (int i = 0; i < SPHERES; i++)
      {
         Sphere s1 = getSphere(i);
         Sphere s2 = morphogen.getSphere(i);
         for (int j = 0; j < s1.sectors.length; j++)
         {
            Sphere.Sector t1 = s1.sectors[j];
            Sphere.Sector t2 = s2.sectors[j];
            for (int k = 0; k < t1.typeDensities.length; k++)
            {
               if (t1.typeDensities[k] != t2.typeDensities[k])
               {
                  return(false);
               }
            }
         }
      }
      return(true);
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
      writer.flush();
   }


   // Load.
   public static Morphogen load(DataInputStream reader) throws EOFException, IOException
   {
      Morphogen m = new Morphogen();

      for (int i = 0; i < SPHERES; i++)
      {
         Sphere s = m.new Sphere();
         m.spheres.add(s);
         for (int j = 0; j < s.sectors.length; j++)
         {
            int           dx = reader.readInt();
            int           dy = reader.readInt();
            int           d  = reader.readInt();
            Sphere.Sector t  = s.new Sector(dx, dy, d);
            for (int k = 0; k < Cell.numTypes; k++)
            {
               t.setTypeDensity(k, reader.readFloat());
            }
            s.sectors[j] = t;
         }
      }
      return(m);
   }
}
