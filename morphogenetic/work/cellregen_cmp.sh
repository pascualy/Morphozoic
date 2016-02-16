# Number of variables:
#
# NUM_CELL_TYPES=3
#
# Neighborhood dimension/Number of neighborhoods:
#   3x3 9x9
# 1 9  81
# 2 54
# 3 81

 for org_dim in 17 21 25 29 33 37 41
 do
    num_neighborhoods=1
   for neighborhood_dim in 9
   do
    java -Xms1024m -classpath morphozoic.jar morphozoic.applications.CellRegenerationOptimizer \
    	-organismDimensions $org_dim $org_dim \
    	-numCellTypes 3 \
    	-randomSeed 4517 \
    	-populationSize 10 \
    	-fitPopulationSize 5 \
    	-numGenerations 10 \
    	-numUpdateSteps 50 \
    	-minNeighborhoodDimension $neighborhood_dim \
    	-maxNeighborhoodDimension $neighborhood_dim \
    	-minNumNeighborhoods $num_neighborhoods \
    	-maxNumNeighborhoods $num_neighborhoods \
    	-minMetamorphDimension 1 \
    	-maxMetamorphDimension 1 \
    	-minMaxCellMetamorphs 1 \
    	-maxMaxCellMetamorphs 3 \
    	-minMetamorphRandomBias 0.0 \
    	-maxMetamorphRandomBias 0.001 \
    	-deltaMetamorphRandomBias 0.001 \
    	-inhibitCompetingMorphogens false > cellregen_cmp_od${org_dim}_nd${neighborhood_dim}_nn${num_neighborhoods}.txt
   done
   neighborhood_dim=3
   for num_neighborhoods in 3
   do
    java -Xms1024m -classpath morphozoic.jar morphozoic.applications.CellRegenerationOptimizer \
    	-organismDimensions $org_dim $org_dim \
    	-numCellTypes 3 \
    	-randomSeed 4517 \
    	-populationSize 10 \
    	-fitPopulationSize 5 \
    	-numGenerations 10 \
    	-numUpdateSteps 50 \
    	-minNeighborhoodDimension $neighborhood_dim \
    	-maxNeighborhoodDimension $neighborhood_dim \
    	-minNumNeighborhoods $num_neighborhoods \
    	-maxNumNeighborhoods $num_neighborhoods \
    	-minMetamorphDimension 1 \
    	-maxMetamorphDimension 1 \
    	-minMaxCellMetamorphs 1 \
    	-maxMaxCellMetamorphs 3 \
    	-minMetamorphRandomBias 0.0 \
    	-maxMetamorphRandomBias 0.001 \
    	-deltaMetamorphRandomBias 0.001 \
    	-inhibitCompetingMorphogens false > cellregen_cmp_od${org_dim}_nd${neighborhood_dim}_nn${num_neighborhoods}.txt
   done
 done

