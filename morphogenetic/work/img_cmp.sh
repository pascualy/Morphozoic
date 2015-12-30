# Number of variables:
#
# NUM_CELL_TYPES=3
#
# Neighborhood dimension/Number of neighborhoods:
#   3x3 9x9
# 1 9  81
# 2 54
# 3 81

 for num_holes in 100
 do
  for max_hole_size in 21
  do
   num_neighborhoods=1
   for neighborhood_dim in 9
   do
    java -Xms1024m -classpath morphozoic.jar morphozoic.applications.ImageRepairOptimizer \
    	-organismDimensions 50 50 \
    	-numCellTypes 3 \
    	-randomSeed 4517 \
    	-populationSize 25 \
    	-fitPopulationSize 5 \
    	-numGenerations 0 \
    	-numUpdateSteps 5 \
    	-minNeighborhoodDimension $neighborhood_dim \
    	-maxNeighborhoodDimension $neighborhood_dim \
    	-minNumNeighborhoods $num_neighborhoods \
    	-maxNumNeighborhoods $num_neighborhoods \
    	-minMetamorphDimension 1 \
    	-maxMetamorphDimension 1 \
    	-minMaxCellMetamorphs 1 \
    	-maxMaxCellMetamorphs 1 \
    	-minMetamorphRandomBias 0.0 \
    	-maxMetamorphRandomBias 0.0 \
    	-deltaMetamorphRandomBias 0.0 \
    	-inhibitCompetingMorphogens false \
    	-numHoles $num_holes  \
  	-maxHoleSize $max_hole_size #> img_cmp_nh${num_holes}_hs${max_hole_size}_nd${neighborhood_dim}_nn${num_neighborhoods}.txt
   done
   neighborhood_dim=3
   for num_neighborhoods in 3
   do
    java -Xms1024m -classpath morphozoic.jar morphozoic.applications.ImageRepairOptimizer \
    	-organismDimensions 50 50 \
    	-numCellTypes 3 \
    	-randomSeed 4517 \
    	-populationSize 25 \
    	-fitPopulationSize 5 \
    	-numGenerations 0 \
    	-numUpdateSteps 5 \
    	-minNeighborhoodDimension $neighborhood_dim \
    	-maxNeighborhoodDimension $neighborhood_dim \
    	-minNumNeighborhoods $num_neighborhoods \
    	-maxNumNeighborhoods $num_neighborhoods \
    	-minMetamorphDimension 1 \
    	-maxMetamorphDimension 1 \
    	-minMaxCellMetamorphs 1 \
    	-maxMaxCellMetamorphs 1 \
    	-minMetamorphRandomBias 0.0 \
    	-maxMetamorphRandomBias 0.0 \
    	-deltaMetamorphRandomBias 0.0 \
    	-inhibitCompetingMorphogens false \
    	-numHoles $num_holes  \
 	-maxHoleSize $max_hole_size #> img_cmp_nh${num_holes}_hs${max_hole_size}_nd${neighborhood_dim}_nn${num_neighborhoods}.txt
   done
  done
 done

