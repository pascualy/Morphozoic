// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import rdtree.RDclient;
import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Parameters;

// Path finder.
public class Pathfinder extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.Pathfinder";

   // Path finding cell neighborhood dimension.
   public static final int DEFAULT_PATH_FINDING_NEIGHBORHOOD_DIMENSION = 19;
   public static int       PATH_FINDING_NEIGHBORHOOD_DIMENSION         = DEFAULT_PATH_FINDING_NEIGHBORHOOD_DIMENSION;

   // Options.
   public static final String OPTIONS = "\n\t[-pathFindingNeighborhoodDimension <path finding neighborhood dimension>]\n\t[-genMetamorphs <save file name>]\n\t[-accumMetamorphs <load/save file name>]\n\t[-execMetamorphs <load file name>]";

   // Metamorph accumulation file.
   public String accumFilename = null;

   // Constructor.
   public Pathfinder(String[] args, Integer id) throws IllegalArgumentException, IOException
   {
      String usage = "Usage: java morphozoic.Morphozoic\n\t[-organism " + ORGANISM_NAME + "]" + morphozoic.Morphozoic.OPTIONS + OPTIONS;

      // Random numbers.
      randomizer = new Random(Parameters.RANDOM_SEED);

      // Get arguments.
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-genMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            genFilename = args[i];
         }
         else if (args[i].equals("-accumMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            accumFilename = args[i];
         }
         else if (args[i].equals("-execMetamorphs"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            execFilename = args[i];
         }
         else if (args[i].equals("-pathFindingNeighborhoodDimension"))
         {
            i++;
            if (i == args.length)
            {
               System.err.println(usage);
               return;
            }
            PATH_FINDING_NEIGHBORHOOD_DIMENSION = Integer.parseInt(args[i]);
            if ((PATH_FINDING_NEIGHBORHOOD_DIMENSION <= 0) || ((PATH_FINDING_NEIGHBORHOOD_DIMENSION % 2) != 1))
            {
               System.err.println("Path finding neighborhood dimension must be positive odd number");
               System.err.println(usage);
               return;
            }
         }
         else
         {
            System.err.println(usage);
            throw new IllegalArgumentException(usage);
         }
      }
      if (Parameters.NUM_CELL_TYPES != 2)
      {
         System.err.println("Number of cell types must equal 2");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      if ((genFilename != null) && (execFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -genMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      if ((genFilename != null) && (accumFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -genMetamorphs and -accumMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      if ((accumFilename != null) && (execFilename != null))
      {
         System.err.println("Mutually exclusive arguments: -accumMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }

      isEditable = true;
      if (accumFilename != null)
      {
         isEditable = false;
         try
         {
            reader = new DataInputStream(new FileInputStream(accumFilename));
            Parameters.loadParms(reader);
            int     x, y;
            boolean eof = false;
            for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  cells[x][y].type = Cell.EMPTY;
               }
            }
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  cells[x][y].type = 0;
                  x = reader.readInt();
               }
            }
            catch (EOFException e)
            {
               eof = true;
            }
            if (!eof)
            {
               Metamorph m;
               if (Parameters.EXEC_METAMORPHS_WITH_SEARCH_TREE)
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                     metamorphSearch.insert((RDclient)m);
                  }
               }
               else
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
               }
            }
         }
         catch (Exception e)
         {
            System.err.println("Cannot load file " + accumFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot load file " + accumFilename +
                                  ":" + e.getMessage());
         }
         genFilename = accumFilename;
      }
      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            Parameters.saveParms(writer);
         }
         catch (Exception e)
         {
            System.err.println("Cannot save file " + genFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot open save file " + genFilename +
                                  ":" + e.getMessage());
         }
      }
      if (execFilename != null)
      {
         try
         {
            reader = new DataInputStream(new FileInputStream(execFilename));
            Parameters.loadParms(reader);
            int     x, y;
            boolean eof = false;
            for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
               {
                  cells[x][y].type = Cell.EMPTY;
               }
            }
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  cells[x][y].type = 0;
                  x = reader.readInt();
               }
            }
            catch (EOFException e)
            {
               eof = true;
            }
            if (!eof)
            {
               Metamorph m;
               if (Parameters.EXEC_METAMORPHS_WITH_SEARCH_TREE)
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                     metamorphSearch.insert((RDclient)m);
                  }
               }
               else
               {
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
               }
            }
         }
         catch (Exception e)
         {
            System.err.println("Cannot load file " + execFilename +
                               ":" + e.getMessage());
            throw new IOException("Cannot load file " + execFilename +
                                  ":" + e.getMessage());
         }
      }
   }


   @Override
   public void update()
   {
      int x, y;

      // Initialize update.
      initUpdate();

      // Save initial cell types?
      if (tick == 0)
      {
         initPathfinding();

         if (genFilename != null)
         {
            isEditable = false;
            try
            {
               for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
               {
                  for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
                  {
                     if (predecessorCells[x][y].type != Cell.EMPTY)
                     {
                        writer.writeInt(x);
                        writer.writeInt(y);
                     }
                  }
               }
               writer.writeInt(-1);
               writer.flush();
            }
            catch (IOException e)
            {
               System.err.println("Cannot save save cell types to " + genFilename + ":" + e.getMessage());
            }
         }
      }

      // Update cells.
      if (execFilename == null)
      {
         // Update path finding.
         updatePathfinding();
         isEditable = false;
      }
      else
      {
         // Execute metamorphs.
         execMetamorphs();
      }
      tick++;

      // Generate metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         saveMetamorphs();
      }
   }


   // Branch.
   class Branch
   {
      Cell from;
      Cell current;
      Cell to;

      Branch(Cell from, Cell to)
      {
         this.from    = from;
         this.current = from;
         this.to      = to;
      }
   }
   ArrayList<Branch> branches;

   // Initialize path finding.
   void initPathfinding()
   {
      // Create initial branches.
      branches = new ArrayList<Branch>();
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            if (cells[x][y].type == 0)
            {
               createBranches(cells[x][y], branches);
            }
         }
      }
   }


   // Create branches from given cell to and from nearby non-empty cells.
   void createBranches(Cell cell, ArrayList<Branch> branches)
   {
      int o  = PATH_FINDING_NEIGHBORHOOD_DIMENSION / 2;
      int cx = cell.x - o;
      int cy = cell.y - o;

      for (int x = 0; x < PATH_FINDING_NEIGHBORHOOD_DIMENSION; x++)
      {
         for (int y = 0; y < PATH_FINDING_NEIGHBORHOOD_DIMENSION; y++)
         {
            int x2 = wrapX(cx + x);
            int y2 = wrapY(cy + y);
            if ((x2 == cell.x) && (y2 == cell.y)) { continue; }
            if (cells[x2][y2].type == 0)
            {
               int     x3        = (x2 + cell.x) / 2;
               int     y3        = (y2 + cell.y) / 2;
               boolean duplicate = false;
               for (Branch b : branches)
               {
                  if ((b.from == cell) && (b.to == cells[x3][y3]))
                  {
                     duplicate = true;
                     break;
                  }
               }
               if (!duplicate)
               {
                  branches.add(new Branch(cell, cells[x3][y3]));
               }
               duplicate = false;
               for (Branch b : branches)
               {
                  if ((b.from == cells[x2][y2]) && (b.to == cells[x3][y3]))
                  {
                     duplicate = true;
                     break;
                  }
               }
               if (!duplicate)
               {
                  branches.add(new Branch(cells[x2][y2], cells[x3][y3]));
               }
            }
         }
      }
   }


   // Update path finding by growing branches.
   void updatePathfinding()
   {
      if (branches.size() == 0) { return; }
      ArrayList<Branch> nextBranches = new ArrayList<Branch>();
      for (int i = 0, n = branches.size(); i < n; i++)
      {
         Branch          branch = branches.get(i);
         ArrayList<Cell> next   = new ArrayList<Cell>();
         float           dist   = 0.0f;
         for (int x = -1; x < 2; x++)
         {
            for (int y = -1; y < 2; y++)
            {
               if ((x == 0) && (y == 0)) { continue; }
               int   x2 = wrapX(branch.current.x + x);
               int   y2 = wrapY(branch.current.y + y);
               float d  = cellDist(x2, y2, branch.to.x, branch.to.y);
               if (next.size() == 0)
               {
                  next.add(cells[x2][y2]);
                  dist = d;
               }
               else if (d < dist)
               {
                  next.clear();
                  next.add(cells[x2][y2]);
                  dist = d;
               }
               else if (d == dist)
               {
                  next.add(cells[x2][y2]);
               }
            }
         }
         if (next.size() > 0)
         {
            // Select and fill next cell in path.
            branch.current = next.get(0);
            if (branch.current.type != 0)
            {
               branch.current.type = 1;
            }

            // Extend branch?
            if (branch.current != branch.to)
            {
               nextBranches.add(branch);
            }
         }
      }
      branches = nextBranches;
   }


   // Cell distance.
   float cellDist(int fromX, int fromY, int toX, int toY)
   {
      int w2 = Parameters.ORGANISM_DIMENSIONS.width / 2;
      int dx = Math.abs(toX - fromX);

      if (dx > w2) { dx = Parameters.ORGANISM_DIMENSIONS.width - dx; }
      int h2 = Parameters.ORGANISM_DIMENSIONS.height / 2;
      int dy = Math.abs(toY - fromY);
      if (dy > h2) { dy = Parameters.ORGANISM_DIMENSIONS.height - dy; }
      return(dx + dy);
   }
}
