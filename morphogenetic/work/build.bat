javac -classpath ../lib/weka.jar -d . ../src/morphozoic/*.java ../src/morphozoic/applications/*.java ../src/rdtree/*.java
copy ..\res\images\*.jpg morphozoic\applications
jar cvfm morphozoic.jar morphozoic.mf morphozoic rdtree
