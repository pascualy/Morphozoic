// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic;

import java.awt.Color;

// Cell.
public class Cell
{
   // Type.
   public static int       DEFAULT_NUM_TYPES = 3;
   public static int       numTypes          = DEFAULT_NUM_TYPES;
   public int              type;
   public static final int EMPTY = -1;

   // Position and orientation.
   public int         x, y;
   public Orientation orientation;

   // Organism.
   public Organism organism;

   // Morphogenetic field.
   public Morphogen morphogen;

   // Cell constructor.
   public Cell(int type, int x, int y,
               Orientation orientation, Organism organism)
   {
      this.type        = type;
      this.x           = x;
      this.y           = y;
      this.orientation = orientation;
      this.organism    = organism;
      morphogen        = null;
   }


   // Clone.
   public Cell clone()
   {
      return(new Cell(type, x, y, orientation, organism));
   }


   // Get color.
   public Color getColor()
   {
      return(getColor(type));
   }


   public static Color getColor(int type)
   {
      if (type == EMPTY)
      {
         return(Color.white);
      }
      else
      {
         float rgb = (1.0f / numTypes) * (float)(type);
         return(new Color(rgb, rgb, rgb));
      }
   }


   // Generate morphogenetic field.
   public void generateMorphogen()
   {
      if (type != EMPTY)
      {
         morphogen = new Morphogen(this);
      }
      else
      {
         morphogen = null;
      }
   }
}
