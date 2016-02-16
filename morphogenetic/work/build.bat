javac -classpath "../lib/weka.jar;../lib/opencv-310.jar" -d . ../src/morphozoic/*.java ../src/morphozoic/applications/*.java ../src/morphozoic/compression/*.java ../src/rdtree/*.java
copy ..\res\images\*.jpg morphozoic\applications
jar cvfm morphozoic.jar morphozoic.mf morphozoic rdtree

