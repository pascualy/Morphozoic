// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

// Metamorph.
public class Metamorph
{
   public Morphogen morphogen;

   public static enum Activity
   {
      DIVISION,
      DEATH,
      TYPE,
      ORIENTATION;
   }
   Activity    activity;
   Cell        cell;
   int         type;
   Orientation orientation;

   // Constructors.
   public Metamorph(Morphogen morphogen, Activity activity)
   {
      this.morphogen = morphogen;
      this.activity  = activity;
   }


   public Metamorph(Morphogen morphogen, Activity activity, Cell cell)
   {
      this.morphogen = morphogen;
      this.activity  = activity;
      this.cell      = cell;
   }


   public Metamorph(Morphogen morphogen, Activity activity, int type)
   {
      this.morphogen = morphogen;
      this.activity  = activity;
      this.type      = type;
   }


   public Metamorph(Morphogen morphogen, Activity activity, Orientation orientation)
   {
      this.morphogen   = morphogen;
      this.activity    = activity;
      this.orientation = orientation;
   }


   // Builders.
   public static Metamorph division(Morphogen morphogen, Cell cell)
   {
      return(new Metamorph(morphogen, Activity.DIVISION, cell));
   }


   public static Metamorph death(Morphogen morphogen)
   {
      return(new Metamorph(morphogen, Activity.DEATH));
   }


   public static Metamorph type(Morphogen morphogen, int type)
   {
      return(new Metamorph(morphogen, Activity.TYPE, type));
   }


   public static Metamorph orientation(Morphogen morphogen, Orientation orientation)
   {
      return(new Metamorph(morphogen, Activity.ORIENTATION, orientation));
   }


   // Execute.
   public void exec(Cell cell)
   {
      switch (activity)
      {
      case DIVISION:
         int x = cell.x + this.cell.x;
         int y = cell.y + this.cell.y;
         Cell[][] cells          = cell.organism.cells;
         cells[x][y].type        = this.cell.type;
         cells[x][y].orientation = this.cell.orientation;
         break;

      case DEATH:
         cell.type = Cell.EMPTY;
         break;

      case TYPE:
         cell.type = type;
         break;

      case ORIENTATION:
         cell.orientation = orientation;
         break;
      }
   }


   // Save.
   public void save(DataOutputStream writer) throws IOException
   {
      morphogen.save(writer);
      switch (activity)
      {
      case DIVISION:
         writer.writeInt(0);
         writer.writeInt(cell.type);
         writer.writeInt(cell.x);
         writer.writeInt(cell.y);
         writer.writeInt(cell.orientation.ordinal());
         break;

      case DEATH:
         writer.writeInt(1);
         break;

      case TYPE:
         writer.writeInt(2);
         writer.writeInt(type);
         break;

      case ORIENTATION:
         writer.writeInt(3);
         writer.writeInt(orientation.ordinal());
         break;
      }
   }


   // Load.
   public static Metamorph load(DataInputStream reader) throws IOException
   {
      int t, o;

      try
      {
         Morphogen morphogen = Morphogen.load(reader);
         int       a         = reader.readInt();
         switch (a)
         {
         case 0:
            t = reader.readInt();
            int x = reader.readInt();
            int y = reader.readInt();
            o = reader.readInt();
            Cell c = new Cell(t, x, y, Orientation.fromInt(o), null);
            return(new Metamorph(morphogen, Activity.DIVISION, c));

         case 1:
            return(new Metamorph(morphogen, Activity.DEATH));

         case 2:
            t = reader.readInt();
            return(new Metamorph(morphogen, Activity.TYPE, t));

         case 3:
            o = reader.readInt();
            return(new Metamorph(morphogen, Activity.ORIENTATION, Orientation.fromInt(o)));
         }
      }
      catch (EOFException e) {
         return(null);
      }
      return(null);
   }
}
