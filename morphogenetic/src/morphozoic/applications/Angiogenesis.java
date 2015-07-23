// For conditions of distribution and use, see copyright notice in Morphozoic.java

package morphozoic.applications;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import morphozoic.Cell;
import morphozoic.Metamorph;
import morphozoic.Organism;
import morphozoic.Parameters;

// Angiogenesis.
public class Angiogenesis extends Organism
{
   public static final String ORGANISM_NAME = "morphozoic.applications.Angiogenesis";
   public static int neighborhoodSize = Parameters.NEIGHBORHOOD_DIMENSION;
   public static int counter =  Parameters.NEIGHBORHOOD_DIMENSION;
   // Options.
   public static final String OPTIONS = "\n\t[-genMetamorphs <save file name>]\n\t[-execMetamorphs <load file name>]";

   // Constructor.
   public Angiogenesis(String[] args, Integer id) throws IllegalArgumentException, IOException
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
         else
         {
            System.err.println(usage);
            throw new IllegalArgumentException(usage);
         }
      }
      if ((genFilename != null) && (execFilename != null))
      {
         System.err.println("Mutually exclusive options: -genMetamorphs and -execMetamorphs");
         System.err.println(usage);
         throw new IllegalArgumentException(usage);
      }
      isEditable = true;
      if (genFilename != null)
      {
         try
         {
            writer = new DataOutputStream(new FileOutputStream(genFilename));
            Parameters.saveParms(writer);
         }
         catch (Exception e)
         {
            System.err.println("Cannot open save file " + genFilename +
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
               while ((m = Metamorph.load(reader)) != null)
               {
                  metamorphs.add(m);
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

      // Save initial cell types?
      if ((tick == 0) && (genFilename != null))
      {
         try
         {
            for (int x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
            {
               for (int y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
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

      // Update cells.
      if (execFilename == null)
      {
         // Step Game of Life.
         step();
      }
      else
      {
         // Execute metamorphs.
         execMetamorphs();
      }
      tick++;
      if (genFilename != null)
      {
         isEditable = false;
      }

      // Generate metamorphs that produce the updated organism.
      if (genFilename != null)
      {
         saveMetamorphs();
      }
   }


   // Step Angiogenesis.
   private void step()
   {
	   if(counter == 0 && neighborhoodSize != 1) neighborhoodSize--; counter = neighborhoodSize;
	   
	   
      int x, y, x2, y2, w, h, count, i, j, l, k;
      int fieldDensity = 0;
      int minDensity   = 10000;
      int minField     = -1;
      // Clear cells.

      // Apply rules.
      w = 1;
      h = 1;

      for (x = 0; x < Parameters.ORGANISM_DIMENSIONS.width; x++)
      {
         for (y = 0; y < Parameters.ORGANISM_DIMENSIONS.height; y++)
         {
        	 if ( predecessorCells[x][y].type != Cell.EMPTY && predecessorCells[x][y].type != 1 ){
        		 for(i = 0; i < 8; i++){ //north,east,south,west,NE,SE,SW,NW
         		 	for(j = 1; j <= (neighborhoodSize + 1)/2; j++  ){ //number of cells perpendicularly away from source
            		 	for(k = -((neighborhoodSize - 1)/2 + j); k <= ((neighborhoodSize - 1)/2 + j); k++   ) {//number of cells laterally away from source
            		 		try {
            		 			if(i == 0 && predecessorCells[x + k][y + j].type != Cell.EMPTY){//north field
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 1 && predecessorCells[x - j][y + k].type != Cell.EMPTY){//east field
            		 				//System.err.print("checking from x: " + x + " y:" + y + "to x: " + (x + j) + " y:" + (y + k) + "\n");
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 2 && predecessorCells[x + k][y - j].type != Cell.EMPTY){//south field
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 3 && predecessorCells[x + j][y + k].type != Cell.EMPTY){//west field
            		 				++fieldDensity;
            		 			}
            		 			k += 1;
            		 			if(		i == 4 && predecessorCells[x - j][y + k].type != Cell.EMPTY){//NE field
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 5 && predecessorCells[x - j][y - k].type != Cell.EMPTY){//SE field
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 6 && predecessorCells[x + j][y - k].type != Cell.EMPTY){//SW field
            		 				++fieldDensity;
            		 			}
            		 			else if(i == 7 && predecessorCells[x + j][y + k].type != Cell.EMPTY){//NW field
            		 				++fieldDensity;
            		 			}
            		 		}
            		 		catch(ArrayIndexOutOfBoundsException e){
             		 		}
            		 	}
         		 	}
         		 	if(fieldDensity < minDensity ) minField = i; minDensity = fieldDensity;
         		          		 	
         		 	
         		 	if(i == 0 && fieldDensity < Parameters.AVERAGE_DENSITY ){
         		 		try{
         		 			if(cells[x][y+1].type != 1)
         		 				cells[x][y+1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 1 && fieldDensity < Parameters.AVERAGE_DENSITY){
         		 		try{
         		 			if(cells[x - 1][y].type != 1)
         		 				cells[x - 1][y].type = 0; 
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 2 && fieldDensity < Parameters.AVERAGE_DENSITY){
         		 		try{
         		 			if(cells[x][y - 1].type != 1)
         		 				cells[x][y - 1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 3 && minDensity < Parameters.AVERAGE_DENSITY){
         		 		try{	
         		 			if(cells[x+1][y].type != 1)
         		 				cells[x+1][y].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 4 && minDensity < Parameters.AVERAGE_DENSITY){
         		 		try{	
         		 			if(cells[x+1][y+1].type != 1)
         		 				cells[x+1][y+1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 5 && minDensity < Parameters.AVERAGE_DENSITY){
         		 		try{	
         		 			if(cells[x+1][y-1].type != 1)
         		 				cells[x+1][y-1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 6 && minDensity < Parameters.AVERAGE_DENSITY){
         		 		try{	
         		 			if(cells[x-1][y-1].type != 1)
         		 				cells[x-1][y-1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	else if(i == 7 && minDensity < Parameters.AVERAGE_DENSITY){
         		 		try{	
         		 			if(cells[x-1][y+1].type != 1)
         		 				cells[x-1][y+1].type = 0;
         		 			cells[x][y].type = 1;
         		 		}
         		 		catch(ArrayIndexOutOfBoundsException e){
         		 		}
         		 	}
         		 	fieldDensity = 0;
        		 }
        	 }
         }
      }
   }
}
