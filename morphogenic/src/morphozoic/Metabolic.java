// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

// Metabolic.
public class Metabolic
{
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
   public Metabolic(Activity activity)
   {
      this.activity = activity;
   }


   public Metabolic(Activity activity, Cell cell)
   {
      this.activity = activity;
      this.cell     = cell;
   }


   public Metabolic(Activity activity, int type)
   {
      this.activity = activity;
      this.type     = type;
   }


   public Metabolic(Activity activity, Orientation orientation)
   {
      this.activity    = activity;
      this.orientation = orientation;
   }


   public static Metabolic division(Cell cell)
   {
      return(new Metabolic(Activity.DIVISION, cell));
   }


   public static Metabolic death()
   {
      return(new Metabolic(Activity.DEATH));
   }


   public static Metabolic type(int type)
   {
      return(new Metabolic(Activity.TYPE, type));
   }


   public static Metabolic orientation(Orientation orientation)
   {
      return(new Metabolic(Activity.ORIENTATION, orientation));
   }
}
