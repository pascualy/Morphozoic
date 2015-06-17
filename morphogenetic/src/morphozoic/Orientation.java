// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

public enum Orientation
{
   NORTH,
   NORTHEAST,
   EAST,
   SOUTHEAST,
   SOUTH,
   SOUTHWEST,
   WEST,
   NORTHWEST;

   private static Orientation[] vals = values();

   // Create orientation from int.
   public static Orientation fromInt(int o)
   {
      switch (o)
      {
      case 0:
         return(Orientation.NORTH);

      case 1:
         return(Orientation.NORTHEAST);

      case 2:
         return(Orientation.EAST);

      case 3:
         return(Orientation.SOUTHEAST);

      case 4:
         return(Orientation.SOUTH);

      case 5:
         return(Orientation.SOUTHWEST);

      case 6:
         return(Orientation.WEST);

      case 7:
         return(Orientation.NORTHWEST);
      }
      return(Orientation.NORTH);
   }


   // Rotate.
   public Orientation rotate(int o)
   {
      return(vals[(8 + this.ordinal() + o) % 8]);
   }
}
