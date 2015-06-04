// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.util.Vector;

import morphozoic.Cell;
import morphozoic.Metabolic;
import morphozoic.Organism;

// Gastrulation.
public class Gastrulation extends Organism
{
   int tick;

   // Predecessor cells.
   public Cell[][] predecessorCells;

   // Constructor.
   public Gastrulation(Integer randomSeed)
   {
      tick             = 0;
      predecessorCells = new Cell[DIMENSIONS.width][DIMENSIONS.height];
   }


   @Override
   public void update()
   {
      int x, y, x2, y2, s, s2;

      // Generate morphogenic fields.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            cells[x][y].generateMorphogen();
         }
      }

      // Create predecessor cells.
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            predecessorCells[x][y]           = cells[x][y].clone();
            predecessorCells[x][y].morphogen = cells[x][y].morphogen;
            cells[x][y].morphogen            = null;
         }
      }

      // Update current cells.
      x = DIMENSIONS.width / 2;
      y = DIMENSIONS.height / 2;
      if (tick < 9)
      {
         s  = (tick * 2) + 1;
         s2 = s / 2;
         for (y2 = 0; y2 < s; y2++)
         {
            for (x2 = 0; x2 < s; x2++)
            {
               cells[x + x2 - s2][y + y2 - s2].type = randomizer.nextInt(Cell.numTypes);
            }
         }
         s -= 2;
         s2 = s / 2;
         for (y2 = 0; y2 < s; y2++)
         {
            for (x2 = 0; x2 < s; x2++)
            {
               cells[x + x2 - s2][y + y2 - s2].type = Cell.EMPTY;
            }
         }
      }
      else if (tick < 20)
      {
         s = x + 4;
         int t = (9 - tick) + 7;
         for (x2 = x - 3; x2 < s; x2++)
         {
            cells[x2][y - t].type = randomizer.nextInt(Cell.numTypes);
         }
         s = x + 3;
         t++;
         for (x2 = x - 2; x2 < s; x2++)
         {
            cells[x2][y - t].type = Cell.EMPTY;
         }
      }
      tick++;

      // Create predecessor metabolic activities that produce the updated organism.
      Vector<Cell> sameParents  = new Vector<Cell>();
      Vector<Cell> otherParents = new Vector<Cell>();
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            if (cells[x][y].type != Cell.EMPTY)
            {
               if (predecessorCells[x][y].type == Cell.EMPTY)
               {
                  // Determine parent of new cell.
                  for (x2 = x - 1; x2 <= x + 1; x2++)
                  {
                     for (y2 = y - 1; y2 < y + 1; y2++)
                     {
                        if ((x2 == x) && (y2 == y)) { continue; }
                        if ((x2 < 0) || (x2 >= DIMENSIONS.width)) { continue; }
                        if ((y2 < 0) || (y2 >= DIMENSIONS.height)) { continue; }
                        if (predecessorCells[x2][y2].type != Cell.EMPTY)
                        {
                           if (predecessorCells[x2][y2].type == cells[x][y].type)
                           {
                              sameParents.add(predecessorCells[x2][y2]);
                           }
                           else
                           {
                              otherParents.add(predecessorCells[x2][y2]);
                           }
                        }
                     }
                  }
                  if (sameParents.size() > 0)
                  {
                     // Prefer to divide parent of same type.
                     Cell parent = sameParents.get(randomizer.nextInt(sameParents.size()));
                     parent.addMetabolic(Metabolic.division(cells[x][y].clone()));
                  }
                  else if (otherParents.size() > 0)
                  {
                     Cell parent = otherParents.get(randomizer.nextInt(otherParents.size()));
                     parent.addMetabolic(Metabolic.division(cells[x][y].clone()));
                  }
               }
               else
               {
                  if (cells[x][y].type != predecessorCells[x][y].type)
                  {
                     // Type change.
                     predecessorCells[x][y].addMetabolic(Metabolic.type(cells[x][y].type));
                  }
                  else if (cells[x][y].orientation != predecessorCells[x][y].orientation)
                  {
                     // Orientation change.
                     predecessorCells[x][y].addMetabolic(Metabolic.orientation(cells[x][y].orientation));
                  }
               }
            }
         }
      }
      for (x = 0; x < DIMENSIONS.width; x++)
      {
         for (y = 0; y < DIMENSIONS.height; y++)
         {
            if (cells[x][y].type == Cell.EMPTY)
            {
               if (predecessorCells[x][y].type != Cell.EMPTY)
               {
                  predecessorCells[x][y].addMetabolic(Metabolic.death());
               }
            }
         }
      }
   }
}
