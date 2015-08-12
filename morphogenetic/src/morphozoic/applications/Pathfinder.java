// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.awt.Color;
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
import morphozoic.Orientation;
import morphozoic.Parameters;

// Path finder.
public class Pathfinder extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.Pathfinder";

   // Path finding cell neighborhood dimension.
   public static final int DEFAULT_PATH_FINDING_NEIGHBORHOOD_DIMENSION = 15;
   public static int       PATH_FINDING_NEIGHBORHOOD_DIMENSION         = DEFAULT_PATH_FINDING_NEIGHBORHOOD_DIMENSION;

   // Options.
   public static final String OPTIONS =
      "\n\t[-pathFindingNeighborhoodDimension <path finding neighborhood dimension>]"
      + "\n\t[-genMetamorphs <save file name>]"
      + "\n\t[-accumMetamorphs <load/save file name> (overrides command-line parameters)]"
      + "\n\t[-execMetamorphs <load file name> (overrides command-line parameters)]";

   // Metamorph accumulation file.
   public String accumFilename = null;

   // Cell types.
   public static final int SOURCE_CELL = 0;
   public static final int TARGET_CELL = 1;
   public static final int BRANCH_CELL = 2;

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

      if (Parameters.NUM_CELL_TYPES != 3)
      {
         System.err.println("Number of cell types must equal 3");
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
      for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
            cells[x][y] = new Cell(Cell.EMPTY, x, y, Orientation.NORTH, this);
         }
      }
      isEditable = true;
      if (accumFilename != null)
      {
         try
         {
            reader = new DataInputStream(new FileInputStream(accumFilename));
            Parameters.load(reader);
            int     x, y, t;
            boolean eof = false;
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  t = reader.readInt();
                  cells[x][y].type = t;
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
               while ((m = Metamorph.load(reader)) != null)
               {
                  metamorphs.add(m);
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
            Parameters.save(writer);
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
            Parameters.load(reader);
            int     x, y, t;
            boolean eof = false;
            try
            {
               x = reader.readInt();
               while (x != -1)
               {
                  y = reader.readInt();
                  t = reader.readInt();
                  cells[x][y].type = t;
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
               switch (Parameters.METAMORPH_EXEC_TYPE)
               {
               case LINEAR_SEARCH:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
                  break;

               case SEARCH_TREE:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                     metamorphSearch.insert((RDclient)m);
                  }
                  break;

               case NEURAL_NETWORK:
                  while ((m = Metamorph.load(reader)) != null)
                  {
                     metamorphs.add(m);
                  }
                  createMetamorphNNs();
                  break;
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
      // Initialize update.
      initUpdate();

      // Save initial configuration?
      if (tick == 0)
      {
         initPathfinding();

         if (genFilename != null)
         {
            isEditable = false;
            saveConfig();
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


   // Save configuration.
   public void saveConfig()
   {
      try
      {
         for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
         {
            for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
            {
               if (cells[x][y].type != Cell.EMPTY)
               {
                  writer.writeInt(x);
                  writer.writeInt(y);
                  writer.writeInt(cells[x][y].type);
               }
            }
         }
         writer.writeInt(-1);
         writer.flush();
         for (Metamorph m : metamorphs)
         {
            m.save(writer);
         }
      }
      catch (IOException e)
      {
         System.err.println("Cannot save configuration: " + e.getMessage());
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
            if (cells[x][y].type == SOURCE_CELL)
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
            if (cells[x2][y2].type == TARGET_CELL)
            {
               branches.add(new Branch(cell, cells[x2][y2]));
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
               int x2 = branch.current.x + x;
               int y2 = branch.current.y + y;
               if ((x2 >= 0) && (x2 < Parameters.ORGANISM_DIMENSIONS.width) &&
                   (y2 >= 0) && (y2 < Parameters.ORGANISM_DIMENSIONS.height))
               {
                  float d = cellDist(x2, y2, branch.to.x, branch.to.y);
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
         }
         if (next.size() > 0)
         {
            // Select and fill next cell in path.
            branch.current = next.get(0);
            if (branch.current.type == Cell.EMPTY)
            {
               branch.current.type = BRANCH_CELL;
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
      int dx = Math.abs(toX - fromX);
      int dy = Math.abs(toY - fromY);

      return(dx + dy);
   }


   // Get color for cell type.
   @Override
   public Color getColor(int type)
   {
      if (type == Cell.EMPTY) { return(Color.white); }
      switch (type)
      {
      case Cell.EMPTY:
         return(Color.white);

      case SOURCE_CELL:
         return(Color.green);

      case TARGET_CELL:
         return(Color.red);

      default:
         return(Color.blue);
      }
   }
}
