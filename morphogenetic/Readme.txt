Morphogenetic fields.

Run the gastrulation application and click on a cell to see the surrounding fields.
Cycle through the neighborhoods with the space bar.
Click within a sector to see its cell type densities.

To generate and save metamorph "templates":
java -jar morphozoic.jar -genMetamorphs metamorphs.dat

To load and run using the metamorph templates:
java -jar morphozoic.jar -execMetamorphs metamorphs.dat

Game of Life application:
java -jar morphozoic.jar -organism morphozoic.applications.GameOfLife

C. elegans application:
java -jar morphozoic.jar -organism morphozoic.applications.Celegans

Path finder application:
java -jar morphozoic.jar -organism morphozoic.applications.Pathfinder

Path finder generalization:
java -classpath morphozoic.jar morphozoic.applications.PathfinderGeneralizer \
-organismDimensions 10 10 -randomSeed 45

Gastrulation evolution:
java -classpath morphozoic.jar morphozoic.applications.MorphEvolver  \
-organismDimensions 15 15 \
-numCellTypes 1 \
-populationSize 50 \
-fitPopulationSize 10 \
-numEvolveEpochs 10 \
-numEpochUpdateSteps 1 \
-numGenerations 50 \
-fitGenerations 10 \
-numMatingOffspring 20 \
-randomSeed 54

Turing reaction-diffusion simulation:
java -classpath morphozoic.jar morphozoic.applications.TuringMorph

Image noise repair:
java -jar morphozoic.jar -organism morphozoic.applications.ImageRepair

Image repair optimization:
java -classpath morphozoic.jar morphozoic.applications.ImageRepairOptimizer

Cell regeneration:
java -jar morphozoic.jar -organism morphozoic.applications.CellRegeneration

Cell regeneration optimization:
java -classpath morphozoic.jar morphozoic.applications.CellRegenerationOptimizer

