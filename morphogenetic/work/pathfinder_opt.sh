for target in 1 2 3
do
  for train in 1 5 10
  do
    java -classpath morphozoic.jar morphozoic.applications.PathfinderOptimizer -organismDimensions 9 9 -numCellTypes 3 -populationSize 5 -numTrainingMorphs $train -numSourceCells 1 -numTargetCells $target -numUpdateSteps 18 > pathfinder_opt_${target}_${train}.txt
  done
done
