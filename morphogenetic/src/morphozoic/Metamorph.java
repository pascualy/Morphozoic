// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;

// Metamorph.
public class Metamorph
{
   // Morphogen.
   public Morphogen morphogen;

   // Target cell configuration.
   public Cell[][] targetCells;

   // Hash code.
   public int hashCode;

   // Constructors.
   public Metamorph(Morphogen morphogen, Cell cell)
   {
      this.morphogen = morphogen;
      targetCells    = new Cell[Morphogen.SECTOR_DIMENSION][Morphogen.SECTOR_DIMENSION];
      Cell[][] cells = cell.organism.cells;
      int o  = Morphogen.SECTOR_DIMENSION / 2;
      int cx = cell.x - o;
      int cy = cell.y - o;
      for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
      {
         for (int y = 0; y < Morphogen.SECTOR_DIMENSION; y++)
         {
            int x2 = cx + x;
            int y2 = cy + y;
            targetCells[x][y]   = cells[x2][y2].clone();
            targetCells[x][y].x = x - o;
            targetCells[x][y].y = y - o;
         }
      }
      hashCode = getHashCode();
   }


   public Metamorph(Morphogen morphogen, Cell[][] targetCells)
   {
      this.morphogen   = morphogen;
      this.targetCells = targetCells;
      hashCode         = getHashCode();
   }


   // Get hash code.
   public int getHashCode()
   {
      Random r = new Random(66);

      for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
      {
         for (int y = 0; y < Morphogen.SECTOR_DIMENSION; y++)
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


   // Execute: overlay cell neighborhood with target cells.
   public void exec(Cell cell)
   {
      Cell[][] cells = cell.organism.cells;
      int w = cell.organism.DIMENSIONS.width;
      int h = cell.organism.DIMENSIONS.height;
      for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
      {
         for (int y = 0; y < Morphogen.SECTOR_DIMENSION; y++)
         {
            int x2 = cell.x + targetCells[x][y].x;
            int y2 = cell.y + targetCells[x][y].y;
            while (x2 < 0) { x2 += w; }
            while (x2 >= w) { x2 -= w; }
            while (y2 < 0) { y2 += h; }
            while (y2 >= h) { y2 -= h; }
            cells[x2][y2].type        = targetCells[x][y].type;
            cells[x2][y2].orientation = targetCells[x][y].orientation;
         }
      }
   }


   // Save.
   public void save(DataOutputStream writer) throws IOException
   {
      morphogen.save(writer);
      for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
      {
         for (int y = 0; y < Morphogen.SECTOR_DIMENSION; y++)
         {
            writer.writeInt(targetCells[x][y].type);
            writer.writeInt(targetCells[x][y].orientation.ordinal());
         }
      }
   }


   // Load.
   public static Metamorph load(DataInputStream reader) throws IOException
   {
      try
      {
         Morphogen morphogen = Morphogen.load(reader);
         Cell[][] targetCells = new Cell[Morphogen.SECTOR_DIMENSION][Morphogen.SECTOR_DIMENSION];
         int d = Morphogen.SECTOR_DIMENSION / 2;
         for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
         {
            for (int y = 0; y < Morphogen.SECTOR_DIMENSION; y++)
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


   // Print.
   public void print()
   {
      System.out.println("Metamorph:");
      morphogen.print();
      System.out.println("  Target cells:");
      for (int y = Morphogen.SECTOR_DIMENSION - 1; y >= 0; y--)
      {
         for (int x = 0; x < Morphogen.SECTOR_DIMENSION; x++)
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
