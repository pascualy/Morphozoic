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

   // Rotate.
   public Orientation rotate(int o)
   {
      return(vals[(8 + this.ordinal() + o) % 8]);
   }
}
