javac -d . ../src/morphozoic/*.java ../src/morphozoic/applications/*.java ../src/rdtree/*.java
cp ../res/images/Celegans*.jpg morphozoic/applications
jar cvfm morphozoic.jar morphozoic.mf morphozoic rdtree

