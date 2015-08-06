Morphogenetic fields.

Run the gastrulation application and click on a cell to see the surrounding fields.
Cycle through the field spheres with the space bar.
Click within a sphere sector to see its cell type densities.

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

