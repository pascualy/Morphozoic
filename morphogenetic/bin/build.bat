javac -d . ../src/morphozoic/*.java ../src/morphozoic/applications/*.java
copy ..\res\images\Celegans*.jpg morphozoic\applications
jar cvfm morphozoic.jar morphozoic.mf morphozoic

